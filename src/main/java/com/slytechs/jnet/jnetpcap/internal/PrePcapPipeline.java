/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.jnet.jnetpcap.internal;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.internal.PcapHeaderABI;

import com.slytechs.jnet.jnetpcap.NetPcap;
import com.slytechs.jnet.jnetpcap.PacketHandler;
import com.slytechs.jnet.jnetpcap.PacketHandler.OfArray;
import com.slytechs.jnet.jnetpcap.PacketHandler.OfBuffer;
import com.slytechs.jnet.jnetpcap.PacketHandler.OfForeign;
import com.slytechs.jnet.jnetpcap.PacketHandler.OfNative;
import com.slytechs.jnet.jnetpcap.processors.PreProcessors;
import com.slytechs.jnet.jnetpcap.processors.PreProcessors.PreProcessorData;
import com.slytechs.jnet.jnetruntime.pipeline.DT;
import com.slytechs.jnet.jnetruntime.pipeline.InputTransformer;
import com.slytechs.jnet.jnetruntime.pipeline.OutputStack;
import com.slytechs.jnet.jnetruntime.pipeline.OutputTransformer;
import com.slytechs.jnet.jnetruntime.pipeline.OutputTransformer.OutputMapper;
import com.slytechs.jnet.jnetruntime.pipeline.Pipeline;
import com.slytechs.jnet.jnetruntime.pipeline.RawDataType;
import com.slytechs.jnet.jnetruntime.util.Registration;
import com.slytechs.jnet.protocol.Packet;
import com.slytechs.jnet.protocol.core.constants.PacketDescriptorType;
import com.slytechs.jnet.protocol.descriptor.PcapDescriptor;

/**
 * @author Mark Bednarczyk
 */
public final class PrePcapPipeline
		extends Pipeline<PreProcessorData>
		implements PreProcessors {

	/**
	 * @author Mark Bednarczyk [mark@slytechs.com]
	 * @author Sly Technologies Inc.
	 */
	public static class PreContext {
		public final PcapHeader pcapHeader;
		public final MemorySegment pcapSegment;
		public final ByteBuffer pcapBuffer;

		public Object user;
		public long packetCount;
		public long lastPacketTs;
		public final PcapHeaderABI abi;

		public PreContext(PcapHeaderABI abi) {
			this.abi = abi;
			this.pcapHeader = new PcapHeader(abi);
			this.pcapSegment = pcapHeader.asMemorySegment();
			this.pcapBuffer = pcapHeader.asByteBuffer();
		}

		public void reset() {
			this.packetCount = 0;
			this.user = null;
		}
	}

	private final OfNative mainInput;
	private final PreContext ctx;

	private final PcapDescriptor pcapDescriptorReusable = new PcapDescriptor();
	private final PcapHeaderABI PCAP_ABI;
	private final OutputStack<PreProcessorData> cbStack;
	private final OutputTransformer<PreProcessorData, OfNative> nativeOutput;
	private final OutputTransformer<PreProcessorData, OfArray<Object>> arrayOutput;
	private final OutputTransformer<PreProcessorData, OfBuffer<Object>> bufferOutput;
	private final OutputTransformer<PreProcessorData, OfForeign<Object>> foreignOutput;
	private final PcapSource pcapSource;

	/**
	 * @param name
	 * @param reducer
	 */
	public PrePcapPipeline(String deviceName, NetPcap pcap, PcapHeaderABI abi, PcapSource source) {
		super(deviceName, new RawDataType<>(PreProcessorData.class));
		this.PCAP_ABI = abi;
		this.pcapSource = source;

		this.ctx = new PreContext(abi);

		var mseg = Arena.ofAuto().allocate(PcapDescriptor.PCAP_DESCRIPTOR_LENGTH);
		this.pcapDescriptorReusable.bind(mseg.asByteBuffer(), mseg);

		this.mainInput = head()
				.addInput("OfNative", this::inputOfNative, new RawDataType<>(OfNative.class))
				.getInputPerma(); // Guaranteed it will never change

		this.cbStack = tail().getOutputStack();
		this.nativeOutput = cbStack.createTransformer(
				"OfNative", this::outputOfNative, new RawDataType<>(OfNative.class));
		this.arrayOutput = cbStack.createTransformer(
				"OfArray", this::outputOfArray, new DT<OfArray<Object>>() {});
		this.bufferOutput = cbStack.createTransformer(
				"OfBuffer", this::outputOfBuffer, new DT<OfBuffer<Object>>() {});
		this.foreignOutput = cbStack.createTransformer(
				"OfForeign", this::outputOfForeign, new DT<OfForeign<Object>>() {});

	}

	private final PreProcessorData outputOfForeign(Supplier<OfForeign<Object>> out) {
		return (header, packet, ctx) -> {
			var cb = out.get();
			cb.handleForeign(ctx.user, header, packet);

			return 1;
		};
	}

	private final PreProcessorData outputOfArray(Supplier<PacketHandler.OfArray<Object>> out) {
		return (header, packet, ctx) -> {
			var hdr = new PcapHeader(header);
			var buf = packet.toArray(ValueLayout.JAVA_BYTE);

			var cb = out.get();
			cb.handleArray(ctx.user, hdr, buf);

			return 1;
		};
	}

	private final PreProcessorData outputOfBuffer(Supplier<OfBuffer<Object>> out) {
		return (header, packet, ctx) -> {
			var hdr = new PcapHeader(header);
			var buf = packet.asByteBuffer();

			var cb = out.get();
			cb.handleBuffer(ctx.user, hdr, buf);

			return 1;
		};
	}

	private final PreProcessorData outputOfNative(Supplier<OfNative> out,
			OutputTransformer<?, ?> output) {
		return (header, packet, ctx) -> {
			var cb = out.get();
			cb.handleNative((MemorySegment) ctx.user, header, packet);

			return 1;
		};
	}

	private final OfNative inputOfNative(Supplier<PreProcessorData> out,
			InputTransformer<?, ?> input) {
		return (_, header, packet) -> {
			if (header.byteSize() == 0)
				header = header.reinterpret(24);

			if (packet.byteSize() == 0)
				packet = packet.reinterpret(PCAP_ABI.captureLength(header));

			ctx.reset();

			ctx.pcapSegment.copyFrom(header);

			var np = out.get();
			long count = np.processNativePacket(header, packet, ctx);
			ctx.packetCount = count;

		};
	}

	private OfNative getOfNativeInput() {
		return this.mainInput;
	}

	@SuppressWarnings("unchecked")
	public <U> long dispatchForeign(long count, OfForeign<U> handler, U user) {

		ctx.user = user;

		foreignOutput.connectNoRegistration((OfForeign<Object>) handler);
		cbStack.push(foreignOutput);

		capturePackets(count);

		cbStack.pop();
		foreignOutput.disconnect();

		return ctx.packetCount;
	}

	public long dispatchNative(long count, OfNative handler, MemorySegment user) {

		ctx.user = user;

		nativeOutput.connectNoRegistration(handler);
		cbStack.push(nativeOutput);

		capturePackets(count);

		cbStack.pop();
		nativeOutput.disconnect();

		return ctx.packetCount;
	}

	@SuppressWarnings("unchecked")
	public <U> long dispatchArray(long count, PacketHandler.OfArray<U> handler, U user) {

		ctx.user = user;

		arrayOutput.connectNoRegistration((OfArray<Object>) handler);
		cbStack.push(arrayOutput);

		capturePackets(count);

		cbStack.pop();
		foreignOutput.disconnect();

		return ctx.packetCount;
	}

	@SuppressWarnings("unchecked")
	public <U> long dispatchBuffer(long count, OfBuffer<U> handler, U user) {

		ctx.user = user;

		bufferOutput.connectNoRegistration((OfBuffer<Object>) handler);
		cbStack.push(bufferOutput);

		capturePackets(count);

		cbStack.pop();
		foreignOutput.disconnect();

		return ctx.packetCount;
	}

	public Registration addOutput(Object id, OfNative handler) {
		var output = cbStack.createTransformer(id, new OutputMapper<PreProcessorData, OfNative>() {

			@Override
			public PreProcessorData createMappedOutput(Supplier<OfNative> sink,
					OutputTransformer<PreProcessorData, OfNative> output) {
				return (header, packet, context) -> {
					sink.get().handleNative(MemorySegment.NULL, header, packet);

					return 1;
				};
			}
		}, new RawDataType<OfNative>(OfNative.class));

		output.connect(handler);

		cbStack.push(output);

		return () -> cbStack.remove(output);
	}

	public boolean nextPacket(Packet packetReference) {
		assert packetReference.descriptor().type() == PacketDescriptorType.PCAP
				: "packet descriptor must be PcapDescriptor type";

		dispatchNative(1, new OfNative() {

			@Override
			public void handleNative(MemorySegment user, MemorySegment header, MemorySegment packet) {
				PcapHeader pcapHeader = new PcapHeader(header);
				long timestamp = pcapHeader.timestamp();
				int caplen = pcapHeader.captureLength();
				int wirelen = pcapHeader.wireLength();

				pcapDescriptorReusable.initDescriptor(timestamp, caplen, wirelen);

				var newPacket = Arena.ofAuto().allocate(packet.byteSize()).copyFrom(packet);

				packetReference.bind(newPacket.asByteBuffer(), newPacket);
				packetReference.descriptor()
						.withBinding(pcapDescriptorReusable.buffer(), pcapDescriptorReusable.memorySegment());

			}

		}, MemorySegment.NULL);

		return ctx.packetCount > 0;
	}

	public long capturePackets(long count) {
		/*
		 * The value 0 has a different meaning - it indicates to process all packets
		 * currently in the buffer (for offline captures) or to wait for and process
		 * packets until a read timeout occurs (for live captures).
		 */
		if (count == -1 || count == 0)
			pcapSource.dispatchNative((int) count, getOfNativeInput(), MemorySegment.NULL);

		else {
			while (count > 0) {
				int count32 = (count > Integer.MAX_VALUE)
						? Integer.MAX_VALUE
						: (int) count;

				int r = pcapSource.dispatchNative(count32, getOfNativeInput(), MemorySegment.NULL);
				if (r < 0)
					break;

				count -= count32;
			}
		}

		return ctx.packetCount;
	}
}

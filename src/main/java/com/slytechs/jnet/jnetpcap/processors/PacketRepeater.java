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
package com.slytechs.jnet.jnetpcap.processors;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import com.slytechs.jnet.jnetpcap.internal.PrePcapPipeline.PreContext;
import com.slytechs.jnet.jnetpcap.processors.PreProcessors.PreProcessorData;
import com.slytechs.jnet.jnetruntime.pipeline.Processor;
import com.slytechs.jnet.jnetruntime.time.TimestampUnit;

/**
 * A packet repeater pre-processor. The repeater is able to repeat, or reinsert
 * a previously seen packet back into the Pcap Pro packet distream any number of
 * times. This allows every packet to be repeated a certain number of times.
 * <p>
 * You can also add a delay or inter-frame-gap between each of the repeated
 * packets, allowing certain amount of spacing. Optionally, you can also the
 * repeater rewrite the "timestamp" field of the pcap header, so that each
 * packet has an updated timestamp that is different from the original packet.
 * </p>
 *
 * @author Mark Bednarczyk
 */
public final class PacketRepeater
		extends Processor<PreProcessorData>
		implements PreProcessorData {

	/** The Constant NAME. */
	public static final String NAME = "PacketRepeater";

	private PacketRepeaterSettings settings = new PacketRepeaterSettings();

	/**
	 * @param priority
	 * @param name
	 */
	public PacketRepeater(long repeatCount) {
		super(PreProcessors.PACKET_REPEATER_PRIORITY, NAME);

		repeatCount(repeatCount);
	}

	public PacketRepeater(PacketRepeaterSettings settings) {
		super(PreProcessors.PACKET_REPEATER_PRIORITY, NAME);

		this.settings.mergeValues(settings);
	}

	/**
	 * Discard all packets. No packets, including the originals, will be sent
	 * through. Setting a repeat count value after making a call to this method,
	 * will reset the discard flag.
	 *
	 * @return this packet repeater
	 */
	public PacketRepeater discardAllPackets() {
		return discardAllPackets(true);
	}

	/**
	 * Discard all packets, conditionally. No packets, including the originals, will
	 * be sent through. Setting a repeat count value after making a call to this
	 * method, will reset the discard flag.
	 *
	 *
	 * @param discard the discard flag which can be enabled or disabled
	 * @return this packet repeater
	 */
	public PacketRepeater discardAllPackets(boolean discard) {
		if (discard)
			rwGuard.writeLocked(() -> settings.REPEAT_COUNT.setValue(-1L, this));

		return this;
	}

	/**
	 * Discard all packets if the discard flag supplier returns true. Causes all
	 * packets, including the original packet to be discarded.
	 *
	 * @param discard a discard flag supplier
	 * @return this packet repeater
	 */
	public PacketRepeater discardAllPackets(BooleanSupplier discard) {
		return rwGuard.writeLocked(() -> discardAllPackets(discard.getAsBoolean()));
	}

	/**
	 * Gets the current repeated packet delay or inter-frame-gap (IFG) if any.
	 *
	 * @param unit the time unit requested
	 * @return the delay
	 */
	public long getIfgForRepeated(TimeUnit unit) {
		return rwGuard.readLocked(() -> unit.convert(settings.IFG.getLong(), TimeUnit.NANOSECONDS));
	}

	/**
	 * Gets the current repeated packet delay or inter-frame-gap if any, with nano
	 * second precision.
	 *
	 * @return the delay nano
	 */
	public long getIfgForRepeatedNano() {
		return rwGuard.readLocked(() -> settings.IFG.getLong());
	}

	/**
	 * Gets the minimum ifg nano.
	 *
	 * @return the minIfgNano
	 */
	public long getMinimumIfgNano() {
		return rwGuard.readLocked(() -> settings.IFG_MIN.getLong());
	}

	/**
	 * Gets the repeat count value.
	 *
	 * @return the repeat count
	 */
	public long getRepeatCount() {
		return rwGuard.readLocked(() -> settings.REPEAT_COUNT.getLong());
	}

	/**
	 * Gets the timestamp unit.
	 *
	 * @return the timestampUnit
	 */
	public TimestampUnit getTimestampUnit() {
		return rwGuard.readLocked(() -> settings.TS_UNIT.getEnum());
	}

	/**
	 * Checks if is rewrite timestamp flag is set.
	 *
	 * @return true, if is rewrite timestamp
	 */
	public boolean isRewriteTimestamp() {
		return rwGuard.readLocked(() -> settings.REWRITE_TIMESTAMP.getBoolean());
	}

	@Override
	public long processNativePacket(MemorySegment header, MemorySegment packet,
			@SuppressWarnings("exports") PreContext ctx) {
		long count = 0;
		long repeatCount = settings.REPEAT_COUNT.getLong();
		long minIfg = settings.IFG_MIN.getLong();
		long ifg = settings.IFG.getLong();
		boolean delay = ifg > 0 || minIfg > 0;
		boolean rewrite = settings.REWRITE_TIMESTAMP.getBoolean();
		var stopWatch = ctx.frameStopwatch;
		var frameHdr = ctx.pcapHeader;

		try {
			for (long c = 0; c < repeatCount; c++) {

				try (var _ = stopWatch.start(frameHdr)) {

					if (delay)
						stopWatch.delayIfg(c == 0 ? minIfg : ifg);

					if (rewrite && delay)
						frameHdr.timestamp(stopWatch.newTsNanos(), TimestampUnit.EPOCH_NANO);

					count += getOutput().processNativePacket(header, packet, ctx);

				}
			}

		} catch (InterruptedException e) {

		}

		return count;
	}

	/**
	 * Sets the repeat count. Each packet captured by pcap is repeated, not
	 * duplicated. The exact original packet is repeatedly sent into the pcap packet
	 * stream. Each of the repeated packets, will be identical to the original
	 * packet being repeated, including its memory addresses for header and packet
	 * data pointers.
	 *
	 * @param count how many times to repeat the original packet, where 0 means 0
	 *              times so only the original packet will be sent, a 1 means 1
	 *              repeat resulting in 2 packets (the original + 1 repeated) will
	 *              be sent, etc.
	 * @return this packet repeater
	 */
	public PacketRepeater repeatCount(long count) {

		rwGuard.writeLocked(() -> {
			if (count < 0)
				throw new IllegalArgumentException("repeat count can not be negative");

			settings.REPEAT_COUNT.setValue(count, this);
			return null;
		}, IllegalArgumentException.class);

		return this;
	}

	/**
	 * Sets the repeat count using a supplier. Each packet captured by pcap is
	 * repeated, not duplicated. The exact original packet is repeatedly sent into
	 * the pcap packet stream. Each of the repeated packets, will be identical to
	 * the original packet being repeated, including its memory addresses for header
	 * and packet data pointers.
	 *
	 * @param count how many times to repeat the original packet, where 0 means 0
	 *              times so only the original packet will be sent, a 1 means 1
	 *              repeat resulting in 2 packets (the original + 1 repeated) will
	 *              be sent, etc.
	 * @return this packet repeater
	 */
	public PacketRepeater repeatCount(LongSupplier count) {
		return repeatCount(count.getAsLong());
	}

	/**
	 * Rewrite timestamp flag. The rewrite timestamp flag, will rewrite the
	 * timestamp in the pcap header for all repeated packets to reflect the
	 * calculated timestamp based on the timestamp of the original packet and how
	 * much delay was used before the repeated packet is sent.
	 * <p>
	 * This flag has no effect on the original packet. Its timestamp is never
	 * modified, only the repeated packets.
	 * </p>
	 *
	 * @param enable enables the rewrite timestamp flag
	 * @return this packet repeater
	 */
	public PacketRepeater rewriteTimestamp(boolean enable) {
		rwGuard.writeLocked(() -> settings.REWRITE_TIMESTAMP.setValue(enable, this));

		return this;
	}

	/**
	 * Sets the delay or inter-frame-gap between the original packet and all of
	 * subsequent repeated packets.
	 *
	 * @param duration the duration or inter-frame-gap
	 * @param unit     the time unit for the delay
	 * @return this packet repeater
	 */
	public PacketRepeater setIfgForRepeated(long duration, TimeUnit unit) {
		rwGuard.writeLocked(() -> settings.IFG.setValue(unit.toNanos(duration), this));

		return this;
	}

	/**
	 * Sets the minimum ifg.
	 *
	 * @param ifg  the ifg
	 * @param unit the unit
	 * @return the packet repeater
	 */
	public PacketRepeater setMinimumIfg(long ifg, TimeUnit unit) {
		Objects.requireNonNull(unit, "unit").toNanos(ifg);

		rwGuard.writeLocked(() -> settings.IFG_MIN.setValue(unit.toNanos(ifg), this));

		return this;
	}

	/**
	 * Sets the repeat count using a supplier. Each packet captured by pcap is
	 * repeated, not duplicated. The exact original packet is repeatedly sent into
	 * the pcap packet stream. Each of the repeated packets, will be identical to
	 * the original packet being repeated, including its memory addresses for header
	 * and packet data pointers.
	 *
	 * @param count how many times to repeat the original packet, where 0 means 0
	 *              times so only the original packet will be sent, a 1 means 1
	 *              repeat resulting in 2 packets (the original + 1 repeated) will
	 *              be sent, etc.
	 * @return this packet repeater
	 */
	public PacketRepeater setRepeatCount(IntSupplier count) {
		return repeatCount(count.getAsInt());
	}

	/**
	 * Sets the timestamp unit.
	 *
	 * @param unit the unit
	 * @return the packet repeater
	 */
	public PacketRepeater setTimestampUnit(TimestampUnit unit) {
		rwGuard.writeLocked(() -> settings.TS_UNIT.setValue(Objects.requireNonNull(unit, "unit"), this));

		return this;
	}

}

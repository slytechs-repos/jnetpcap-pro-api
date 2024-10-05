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
package com.slytechs.jnet.jnetpcap;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;

import org.jnetpcap.PcapHandler.OfMemorySegment;
import org.jnetpcap.internal.PcapHeaderABI;

import com.slytechs.jnet.jnetruntime.pipeline.AbstractProcessor;
import com.slytechs.jnet.jnetruntime.pipeline.Pipeline;
import com.slytechs.jnet.jnetruntime.time.TimestampUnit;
import com.slytechs.jnet.jnetruntime.util.SystemProperties;

/**
 * The Class PacketPlayer.
 *
 * @author Mark Bednarczyk
 */
public class PacketPlayer
		extends AbstractProcessor<OfMemorySegment<Object>, PacketPlayer>
		implements OfMemorySegment<Object> {

	/** The Constant PREFIX. */
	private static final String PREFIX = "packet.player";

	/** The Constant PROPERTY_PACKET_PLAYER_ENABLE. */
	public static final String PROPERTY_PACKET_PLAYER_ENABLE = PREFIX + ".enable";

	/** The Constant PROPERTY_PACKET_PLAYER_SYNC. */
	public static final String PROPERTY_PACKET_PLAYER_SYNC = PREFIX + ".sync";

	/** The Constant PROPERTY_PACKET_PLAYER_SPEED. */
	public static final String PROPERTY_PACKET_PLAYER_SPEED = PREFIX + ".speed";

	/** The sync. */
	private boolean sync = SystemProperties.boolValue(PROPERTY_PACKET_PLAYER_SYNC, true);

	/** The speed. */
	private double speed = SystemProperties.doubleValue(PROPERTY_PACKET_PLAYER_SPEED, 1.0);

	/** The reference time nano. */
	private long referenceTimeNano;

	/** The timestamp unit. */
	private TimestampUnit timestampUnit = TimestampUnit.PCAP_MICRO;

	/** The abi. */
	private PcapHeaderABI abi = PcapHeaderABI.selectOfflineAbi(false);

	/** The min ifg nano. */
	private long minIfgNano = 0;

	/** The max ifg nano. */
	private long maxIfgNano = Long.MAX_VALUE;

	/** The Constant NAME. */
	public static final String NAME = "packet-player";

	/**
	 * Instantiates a new packet player.
	 *
	 * @param pipeline the pipeline
	 * @param priority the priority
	 */
	public PacketPlayer(Pipeline<OfMemorySegment<Object>, ?> pipeline, int priority) {
		super(pipeline, priority, NAME, PcapDataType.PCAP_RAW_PACKET);
	}

	/**
	 * Use current time.
	 *
	 * @return the packet player
	 */
	public PacketPlayer useCurrentTime() {
		setReferenceTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);

		return this;
	}

	/**
	 * Sets the reference time nano.
	 *
	 * @param timeNano the time nano
	 * @return the packet player
	 */
	public PacketPlayer setReferenceTimeNano(long timeNano) {
		this.referenceTimeNano = timeNano;

		return this;
	}

	/**
	 * Sets the reference time.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the packet player
	 */
	public PacketPlayer setReferenceTime(long time, TimeUnit unit) {
		this.referenceTimeNano = unit.toNanos(time);

		return this;
	}

	/**
	 * Sets the timestamp unit.
	 *
	 * @param unit the unit
	 * @return the packet player
	 */
	public PacketPlayer setTimestampUnit(TimestampUnit unit) {
		this.timestampUnit = unit;

		return this;
	}

	/**
	 * Gets the reference time nano.
	 *
	 * @return the reference time nano
	 */
	public long getReferenceTimeNano() {
		return referenceTimeNano;
	}

	/**
	 * Sync timestamp.
	 *
	 * @param sync the sync
	 * @return the packet player
	 */
	public PacketPlayer syncTimestamp(boolean sync) {
		this.sync = sync;

		return this;
	}

	/**
	 * Preserve ifg.
	 *
	 * @param sync the sync
	 * @return the packet player
	 */
	public PacketPlayer preserveIfg(boolean sync) {
		this.sync = sync;

		return this;
	}

	/**
	 * Sets the min ifg.
	 *
	 * @param duration the duration
	 * @param unit     the unit
	 * @return the packet player
	 */
	public PacketPlayer setMinIfg(long duration, TimeUnit unit) {
		this.minIfgNano = unit.toNanos(duration);
		return this;
	}

	/**
	 * Sets the max ifg.
	 *
	 * @param duration the duration
	 * @param unit     the unit
	 * @return the packet player
	 */
	public PacketPlayer setMaxIfg(long duration, TimeUnit unit) {
		this.maxIfgNano = unit.toNanos(duration);

		return this;
	}

	/**
	 * Play.
	 *
	 * @param speed the speed
	 * @return the packet player
	 */
	public PacketPlayer play(double speed) {
		if (speed < 0) // Playing backwards not supported
			throw new IllegalArgumentException("negative speed not allowed, playback backwards not supported");

		return this;
	}

	/**
	 * Gets the speed.
	 *
	 * @return the speed
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Checks if is sync.
	 *
	 * @return true, if is sync
	 */
	public boolean isSync() {
		return sync;
	}

	/**
	 * Gets the timestamp unit.
	 *
	 * @return the timestampUnit
	 */
	public TimestampUnit getTimestampUnit() {
		return timestampUnit;
	}

	/**
	 * Gets the abi.
	 *
	 * @return the abi
	 */
	public PcapHeaderABI getAbi() {
		return abi;
	}

	/**
	 * Emulate real time.
	 *
	 * @param b the b
	 * @return the packet player
	 */
	public PacketPlayer emulateRealTime(boolean b) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * Handle segment.
	 *
	 * @param user   the user
	 * @param header the header
	 * @param Packet the packet
	 * @see org.jnetpcap.PcapHandler.OfMemorySegment#handleSegment(java.lang.Object,
	 *      java.lang.foreign.MemorySegment, java.lang.foreign.MemorySegment)
	 */
	@Override
	public void handleSegment(Object user, MemorySegment header, MemorySegment Packet) {
		outputData().handleSegment(user, header, Packet);
	}

}

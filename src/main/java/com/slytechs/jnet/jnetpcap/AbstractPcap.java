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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.jnetpcap.BpFilter;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapActivatedException;
import org.jnetpcap.PcapDumper;
import org.jnetpcap.PcapException;
import org.jnetpcap.PcapHandler.NativeCallback;
import org.jnetpcap.PcapHandler.OfArray;
import org.jnetpcap.PcapHandler.OfMemorySegment;
import org.jnetpcap.PcapStat;
import org.jnetpcap.constant.PcapDirection;
import org.jnetpcap.constant.PcapDlt;
import org.jnetpcap.constant.PcapTStampPrecision;
import org.jnetpcap.constant.PcapTstampType;
import org.jnetpcap.internal.PcapHeaderABI;
import org.jnetpcap.util.PcapPacketRef;

/**
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class AbstractPcap {

	private final Pcap delegate;
	private final PcapType pcapType;

	public AbstractPcap(Pcap delegate, PcapType pcapType) {
		this.delegate = delegate;
		this.pcapType = pcapType;
	}

	protected PcapType pcapType() {
		return pcapType;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * @throws PcapActivatedException
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#activate()
	 */
	public void activate() throws PcapActivatedException, PcapException {
		delegate.activate();
	}

	/**
	 * 
	 * @see org.jnetpcap.Pcap#breakloop()
	 */
	public void breakloop() {
		delegate.breakloop();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#canSetRfmon()
	 */
	public boolean canSetRfmon() throws PcapException {
		return delegate.canSetRfmon();
	}

	/**
	 * 
	 * @see org.jnetpcap.Pcap#close()
	 */
	public void close() {
		delegate.close();
	}

	/**
	 * @param str
	 * @param optimize
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#compile(java.lang.String, boolean)
	 */
	public BpFilter compile(String str, boolean optimize) throws PcapException {
		return delegate.compile(str, optimize);
	}

	/**
	 * @param str
	 * @param optimize
	 * @param netmask
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#compile(java.lang.String, boolean, int)
	 */
	public BpFilter compile(String str, boolean optimize, int netmask) throws PcapException {
		return delegate.compile(str, optimize, netmask);
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#datalink()
	 */
	public PcapDlt datalink() throws PcapException {
		return delegate.datalink();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#dataLinkExt()
	 */
	public PcapDlt dataLinkExt() throws PcapException {
		return delegate.dataLinkExt();
	}

	/**
	 * @param <U>
	 * @param count
	 * @param pcapDumper
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#dispatch(int, org.jnetpcap.PcapDumper)
	 */
	public <U> int dispatch(int count, PcapDumper pcapDumper) throws PcapException {
		return delegate.dispatch(count, pcapDumper);
	}

	/**
	 * @param count
	 * @param handler
	 * @param user
	 * @return
	 * @see org.jnetpcap.Pcap#dispatch(int, org.jnetpcap.PcapHandler.NativeCallback,
	 *      java.lang.foreign.MemorySegment)
	 */
	protected int dispatch(int count, NativeCallback handler, MemorySegment user) {
		return delegate.dispatch(count, handler, user);
	}

	/**
	 * @param fname
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#dumpOpen(java.lang.String)
	 */
	public PcapDumper dumpOpen(String fname) throws PcapException {
		return delegate.dumpOpen(fname);
	}

	/**
	 * @return
	 * @see org.jnetpcap.Pcap#geterr()
	 */
	public String geterr() {
		return delegate.geterr();
	}

	/**
	 * @return
	 * @see org.jnetpcap.Pcap#getName()
	 */
	public final String getName() {
		return delegate.getName();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#getNonBlock()
	 */
	public boolean getNonBlock() throws PcapException {
		return delegate.getNonBlock();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#getTstampPrecision()
	 */
	public PcapTStampPrecision getTstampPrecision() throws PcapException {
		return delegate.getTstampPrecision();
	}

	/**
	 * @param packet
	 * @param length
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#inject(java.lang.foreign.MemorySegment, int)
	 */
	public int inject(MemorySegment packet, int length) throws PcapException {
		return delegate.inject(packet, length);
	}

	/**
	 * @param array
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#inject(byte[])
	 */
	public final int inject(byte[] array) throws PcapException {
		return delegate.inject(array);
	}

	/**
	 * @param array
	 * @param offset
	 * @param length
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#inject(byte[], int, int)
	 */
	public final int inject(byte[] array, int offset, int length) throws PcapException {
		return delegate.inject(array, offset, length);
	}

	/**
	 * @param buf
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#inject(java.nio.ByteBuffer)
	 */
	public final int inject(ByteBuffer buf) throws PcapException {
		return delegate.inject(buf);
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#isSwapped()
	 */
	public boolean isSwapped() throws PcapException {
		return delegate.isSwapped();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#listDataLinks()
	 */
	public List<PcapDlt> listDataLinks() throws PcapException {
		return delegate.listDataLinks();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#listTstampTypes()
	 */
	public List<PcapTstampType> listTstampTypes() throws PcapException {
		return delegate.listTstampTypes();
	}

	/**
	 * @param <U>
	 * @param count
	 * @param pcapDumper
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#loop(int, org.jnetpcap.PcapDumper)
	 */
	public <U> int loop(int count, PcapDumper pcapDumper) throws PcapException {
		return delegate.loop(count, pcapDumper);
	}

	/**
	 * @param <U>
	 * @param count
	 * @param handler
	 * @param user
	 * @return
	 * @see org.jnetpcap.Pcap#loop(int, org.jnetpcap.PcapHandler.NativeCallback,
	 *      java.lang.foreign.MemorySegment)
	 */
	public <U> int loop(int count, NativeCallback handler, MemorySegment user) {
		return delegate.loop(count, handler, user);
	}

	/**
	 * @param <U>
	 * @param count
	 * @param handler
	 * @param user
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#loop(int, org.jnetpcap.PcapHandler.OfArray,
	 *      java.lang.Object)
	 */
	public <U> int loop(int count, OfArray<U> handler, U user) throws PcapException {
		return delegate.loop(count, handler, user);
	}

	/**
	 * @param <U>
	 * @param count
	 * @param handler
	 * @param user
	 * @return
	 * @see org.jnetpcap.Pcap#loop(int, org.jnetpcap.PcapHandler.OfMemorySegment,
	 *      java.lang.Object)
	 */
	public <U> int loop(int count, OfMemorySegment<U> handler, U user) {
		return delegate.loop(count, handler, user);
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#majorVersion()
	 */
	public int majorVersion() throws PcapException {
		return delegate.majorVersion();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#minorVersion()
	 */
	public int minorVersion() throws PcapException {
		return delegate.minorVersion();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#next()
	 */
	public PcapPacketRef next() throws PcapException {
		return delegate.next();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @throws TimeoutException
	 * @see org.jnetpcap.Pcap#nextEx()
	 */
	public PcapPacketRef nextEx() throws PcapException, TimeoutException {
		return delegate.nextEx();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#order()
	 */
	public final ByteOrder order() throws PcapException {
		return delegate.order();
	}

	/**
	 * @param prefix
	 * @return
	 * @see org.jnetpcap.Pcap#perror(java.lang.String)
	 */
	public Pcap perror(String prefix) {
		return delegate.perror(prefix);
	}

	/**
	 * @param packet
	 * @param length
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#sendPacket(java.lang.foreign.MemorySegment, int)
	 */
	public void sendPacket(MemorySegment packet, int length) throws PcapException {
		delegate.sendPacket(packet, length);
	}

	/**
	 * @param buf
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#sendPacket(byte[])
	 */
	public final void sendPacket(byte[] buf) throws PcapException {
		delegate.sendPacket(buf);
	}

	/**
	 * @param buf
	 * @param offset
	 * @param length
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#sendPacket(byte[], int, int)
	 */
	public final void sendPacket(byte[] buf, int offset, int length) throws PcapException {
		delegate.sendPacket(buf, offset, length);
	}

	/**
	 * @param buf
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#sendPacket(java.nio.ByteBuffer)
	 */
	public final void sendPacket(ByteBuffer buf) throws PcapException {
		delegate.sendPacket(buf);
	}

	/**
	 * @param bufferSize
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setBufferSize(int)
	 */
	public Pcap setBufferSize(int bufferSize) throws PcapException {
		return delegate.setBufferSize(bufferSize);
	}

	/**
	 * @param dlt
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setDatalink(int)
	 */
	public Pcap setDatalink(int dlt) throws PcapException {
		return delegate.setDatalink(dlt);
	}

	/**
	 * @param dlt
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setDatalink(java.util.Optional)
	 */
	public Pcap setDatalink(Optional<PcapDlt> dlt) throws PcapException {
		return delegate.setDatalink(dlt);
	}

	/**
	 * @param dlt
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setDatalink(org.jnetpcap.constant.PcapDlt)
	 */
	public Pcap setDatalink(PcapDlt dlt) throws PcapException {
		return delegate.setDatalink(dlt);
	}

	/**
	 * @param dir
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setDirection(int)
	 */
	public Pcap setDirection(int dir) throws PcapException {
		return delegate.setDirection(dir);
	}

	/**
	 * @param dir
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setDirection(java.util.Optional)
	 */
	public Pcap setDirection(Optional<PcapDirection> dir) throws PcapException {
		return delegate.setDirection(dir);
	}

	/**
	 * @param dir
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setDirection(org.jnetpcap.constant.PcapDirection)
	 */
	public Pcap setDirection(PcapDirection dir) throws PcapException {
		return delegate.setDirection(dir);
	}

	/**
	 * @param bpfProgram
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setFilter(org.jnetpcap.BpFilter)
	 */
	public Pcap setFilter(BpFilter bpfProgram) throws PcapException {
		return delegate.setFilter(bpfProgram);
	}

	/**
	 * @param bpfProgram
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setFilter(java.util.Optional)
	 */
	public Pcap setFilter(Optional<BpFilter> bpfProgram) throws PcapException {
		return delegate.setFilter(bpfProgram);
	}

	/**
	 * @param enable
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setImmediateMode(boolean)
	 */
	public Pcap setImmediateMode(boolean enable) throws PcapException {
		return delegate.setImmediateMode(enable);
	}

	/**
	 * @param blockMode
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setNonBlock(boolean)
	 */
	public Pcap setNonBlock(boolean blockMode) throws PcapException {
		return delegate.setNonBlock(blockMode);
	}

	/**
	 * @param promiscousMode
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setPromisc(boolean)
	 */
	public Pcap setPromisc(boolean promiscousMode) throws PcapException {
		return delegate.setPromisc(promiscousMode);
	}

	/**
	 * @param rfMonitor
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setRfmon(boolean)
	 */
	public Pcap setRfmon(boolean rfMonitor) throws PcapException {
		return delegate.setRfmon(rfMonitor);
	}

	/**
	 * @param snaplen
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setSnaplen(int)
	 */
	public Pcap setSnaplen(int snaplen) throws PcapException {
		return delegate.setSnaplen(snaplen);
	}

	/**
	 * @param timeoutInMillis
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setTimeout(int)
	 */
	public Pcap setTimeout(int timeoutInMillis) throws PcapException {
		return delegate.setTimeout(timeoutInMillis);
	}

	/**
	 * @param precision
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setTstampPrecision(org.jnetpcap.constant.PcapTStampPrecision)
	 */
	public Pcap setTstampPrecision(PcapTStampPrecision precision) throws PcapException {
		return delegate.setTstampPrecision(precision);
	}

	/**
	 * @param type
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#setTstampType(org.jnetpcap.constant.PcapTstampType)
	 */
	public Pcap setTstampType(PcapTstampType type) throws PcapException {
		return delegate.setTstampType(type);
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#snapshot()
	 */
	public int snapshot() throws PcapException {
		return delegate.snapshot();
	}

	/**
	 * @return
	 * @throws PcapException
	 * @see org.jnetpcap.Pcap#stats()
	 */
	public PcapStat stats() throws PcapException {
		return delegate.stats();
	}

	/**
	 * @return
	 * @see org.jnetpcap.Pcap#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}

	/**
	 * @return
	 * @see org.jnetpcap.Pcap#getPcapHeaderABI()
	 */
	public PcapHeaderABI getPcapHeaderABI() {
		return delegate.getPcapHeaderABI();
	}

	/**
	 * @param exceptionHandler
	 * @return
	 * @see org.jnetpcap.Pcap#setUncaughtExceptionHandler(java.util.function.Consumer)
	 */
	public Pcap setUncaughtExceptionHandler(Consumer<? super Throwable> exceptionHandler) {
		return delegate.setUncaughtExceptionHandler(exceptionHandler);
	}

	/**
	 * @param exceptionHandler
	 * @return
	 * @see org.jnetpcap.Pcap#setUncaughtExceptionHandler(java.lang.Thread.UncaughtExceptionHandler)
	 */
	public Pcap setUncaughtExceptionHandler(UncaughtExceptionHandler exceptionHandler) {
		return delegate.setUncaughtExceptionHandler(exceptionHandler);
	}
}

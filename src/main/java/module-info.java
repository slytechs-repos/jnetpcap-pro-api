/*
 * Sly Technologies Free License
 * 
 * Copyright 2023 Sly Technologies Inc.
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

/**
 * Provides packet dissection and IP fragment reassembly services.
 * 
 * @author Mark Bednarczyk.
 */
module com.slytechs.jnet.jnetpcap.api {
	exports com.slytechs.jnet.jnetpcap.api;
	exports com.slytechs.jnet.jnetpcap.api.processors;
//	exports com.slytechs.jnet.jnetpcap.api.processor.packet;
//	exports com.slytechs.jnet.jnetpcap.api.processor.protocol;

	requires transitive org.jnetpcap;
	requires transitive com.slytechs.jnet.protocol.api;
	requires transitive com.slytechs.jnet.platform.api;
}
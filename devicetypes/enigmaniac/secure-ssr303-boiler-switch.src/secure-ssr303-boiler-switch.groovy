/**
 *  Copyright 2015 SmartThings
 *  Modified 2017 TLovelock
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Device Handler for a Secure SSR303 (aka Horstmann ASR-ZW) Boiler switch
 *
 *  Standard ST Z-Wave switch modified to turn this device on and off. This device identified as a Thermostat (as opposed to a switch).
 *
 *  http://www.vesternet.com/z-wave-horstmann-z-wave-controlled-boiler-receiver-hrt
 *
 *  Note that the Horstmann HRT4-ZW and the ASR-ZW are being sold today as a 'Secure SRT321' (Thermostat) and 'Secure SSR303' (relay switch). 
 *  This DH allows the relay switch to be controlled directly by ST. To clarify, this DH is for the ASR-ZW or the SSR303. 
 * 
 */
metadata {
	definition (name: "Secure SSR303 Boiler Switch", namespace: "enigmaniac", author: "Tim Lovelock") {
		capability "Actuator"
 		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"

      fingerprint mfr:"0059", prod:"0003", deviceJoinName: "Secure SSR303 Boiler Switch"

	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
	}

	// tile definitions
	tiles(scale: 2) {

multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'Heating', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#aa2222"
				attributeState "off", label: 'Eco Mode', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#cce6ff"
			}
		}


/*
multiAttributeTile(name:"onff", type:"lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: 'Idle', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "on", label: 'Heating', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
    //            attributeState "turningOn", label: 'Turning on Heat', icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState: "turningOff"
    //            attributeState "turningOff", label: 'Going Idle', icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState: "turningOn"
    //				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
    //				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}
*/
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","refresh"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x70: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	} else {
		log.debug "Parse returned ${result?.descriptionText}"
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
	[name: "indicatorStatus", value: value, display: false]
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	[name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", cmd.manufacturerName)
	createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	delayBetween([
		zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

def off() {
	delayBetween([
		zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

/*

def off() {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
    
*/

def poll() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	], 100)
}

def refresh() {
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	], 100)
}

/**
 *  Spruce Web Connect
 *  v1.0
 *
 *  Copyright 2017 Plaid Systems
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
 */
definition(
    name: "Spruce Web Connect",
    namespace: "plaidsystems",
    author: "Plaid Systems",
    description: "Connect Spruce devices to the Spruce Cloud",
    category: "",
    iconUrl: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX2Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    iconX3Url: "http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png",
    oauth: true)
{
	appSetting "clientId"
	appSetting "clientSecret"
    appSetting "serverUrl"
}

preferences (oauthPage: "authPage") {
	page(name: "authPage")
 	page(name: "pageConnect")
    page(name: "pageDevices")
    page(name: "pageUnsetKey")
}

def authPage() {
    if(!state.accessToken) createAccessToken()
    
    if(!state.key)    {
		pageConnect()
    }
    else {
    	pageDevices()
    }
}

def pageConnect() {
    if(!state.key)    {
		def spruce_oauth_url = "https://app.spruceirrigation.com/connect-smartthings?access_token=${state.accessToken}&logout"
        dynamicPage(name: "pageConnect", title: "Connect to Spruce",  uninstall: false, install:false) { 
            section {
                href url:"https://app.spruceirrigation.com/register?gateway=smartthings", style:"embedded", required:false, title:"Register", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Spruce account is required."
            } 
            section {
                href url: spruce_oauth_url, style:"embedded", required:false, title:"Login to Spruce Cloud", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Login to grant access"
            }
        }
    }
    else {
    	pageDevices()
    }
}

def pageDevices() {
    if(!state.key)    {
		pageConnect()
    }
    else {
    	dynamicPage(name: "pageDevices", uninstall: true, install:true) { 
            section("Select Spruce Controllers to connect:") {
                input "switches", "capability.switch", title: "Spruce Irrigation Controller:", required: false, multiple: true 
            }
            section("Select Spruce Sensors to connect:") {
                input "sensors", "capability.relativeHumidityMeasurement", title: "Spruce Moisture sensors:", required: false, multiple: true 
            }
            section ("Tap Done to save changes to Spruce Cloud"){
                href url:"https://app.spruceirrigation.com/devices", style:"external", required:false, title:"Spruce WebApp", image: 'http://www.plaidsystems.com/smartthings/st_spruce_leaf_250f.png', description: "Click to visit app.spruceirrigation.com"
            }      
            section {
                href page: "pageUnsetKey", title:"Reset Login", description: "Tap to forget Spruce API key and re-start login. For troubleshooting only."
            }          
        }   
    }
}

def pageUnsetKey() {
	state.key = null
    dynamicPage(name: "pageUnsetKey", uninstall: false, install:false) { 
        section {
            paragraph "Spruce API key forgotten. Go back to re-start connect process."  
        }          
    }   
}

def oauthInitUrl() /*not used*/
{
    log.debug "oauthInitUrl"    
    def oauthClientId = appSettings.clientId
	def oauthClientSecret = appSettings.clientSecret
	def oauth_url = "https://app.spruceirrigation.com/connect-smartthings?client=${oauthClientId}&secret=${oauthClientSecret}"
    
    log.debug(oauthClientId)
    log.debug(oauthClientSecret)
 	
	return oauth_url	
}

mappings {
  path("/schedule") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/schedule/:command") {
    action: [
      POST: "setSchedule"
    ]
  }
  path("/delete/:command") {
    action: [
      POST: "deleteSchedule"
    ]
  }
  path("/zonetime/:command") {
    action: [
      PUT: "zonetimes"
    ]
  }
  path("/zoneoption/:command") {
    action: [
      PUT: "zoneoptions"
    ]
  }
  path("/run/:command") {
    action: [
      POST: "runZone",
      GET: "runZone"
    ]
  }
  path("/apikey/:command") {
    action: [
      POST: "setKey"
    ]
  }
  path("/delay/:command") {
    action: [
      POST: "setDelay"
    ]
  }
}


def installed() {
	log.debug "Installed with settings: ${settings}"	
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	//unsubscribe()
	initialize()
    //getSchedule()
}

def initialize() {	
    log.debug "initialize"    
    if (settings.switches) getSwitches()
    if (settings.sensors) getSensors()
    //add devices to web
    if(state.key) addDevicesWeb()	
}

//add devices to web
def addDevicesWeb(){
	
    //add controllers to web
    state.switches.each{
    	def PUTparams = [
            uri: "http://api.spruceirrigation.com/v1/device/" + it.value,
            headers: [ 	"Authorization": state.key],        
            body: [
                nickname: it.key,
                type: "CO",
                gateway: "smartthings",
                num_zones: "16"
                ]
        ]    
        log.debug PUTparams
        try{
            httpPut(PUTparams){
                resp -> //resp.data {
                    log.debug "${resp.data}"               
                }                
            } 
        catch (e) {
            log.debug "send DB error: $e"
            }
    }
	
    //add sensors to web	
    state.sensors.each{
    	def PUTparams = [
            uri: "http://api.spruceirrigation.com/v1/device/" + it.value,
            headers: [ 	"Authorization": state.key],        
            body: [
                nickname: it.key,
                type: "MS",
                gateway: "smartthings"
                ]
        ]    
        try{
            httpPut(PUTparams){
                resp -> //resp.data {
                    log.debug "${resp.data}"               
                }                
            } 
        catch (e) {
            log.debug "send DB error: $e"
            }
            }


}

//switch subscriptions
def getSwitches(){
	log.debug "getSwitches: " + settings.switches    
    
    state.switches = [:]
    settings.switches.each{
    	state.switches[it]= (it.device.zigbeeId)
        }
    
    subscribe(settings.switches, "switch", switchHandler)
    subscribe(settings.switches, "switch1", switchHandler)
    subscribe(settings.switches, "switch2", switchHandler)
    subscribe(settings.switches, "switch3", switchHandler)
    subscribe(settings.switches, "switch4", switchHandler)
    subscribe(settings.switches, "switch5", switchHandler)
    subscribe(settings.switches, "switch6", switchHandler)
    subscribe(settings.switches, "switch7", switchHandler)
    subscribe(settings.switches, "switch8", switchHandler)
    subscribe(settings.switches, "switch9", switchHandler)
    subscribe(settings.switches, "switch10", switchHandler)
    subscribe(settings.switches, "switch11", switchHandler)
    subscribe(settings.switches, "switch12", switchHandler)
    subscribe(settings.switches, "switch13", switchHandler)
    subscribe(settings.switches, "switch14", switchHandler)
    subscribe(settings.switches, "switch15", switchHandler)
    subscribe(settings.switches, "switch16", switchHandler)
    subscribe(settings.switches, "rainsensor", switchHandler)
    
}

//sensor subscriptions
def getSensors(){    
    log.debug "getSensors: " + settings.sensors    
    
    state.sensors = [:]    
    settings.sensors.each{
    	state.sensors[it]= (it.device.zigbeeId)
        }
    
    subscribe(settings.sensors, "humidity", sensorHandler)
    subscribe(settings.sensors, "temperature", sensorHandler)
    subscribe(settings.sensors, "battery", sensorHandler)
    
}

//set spruce api key
def setKey(){
	log.debug "setkey: " + params.command
    
	state.key = params.command    
    if (state.key && state.key == params.command) {
    	log.debug "API key set, get schedules"
        //getSchedule()
        return [error: false, data: 'key set']
        }
    else return [error: true, data: 'key not set']
    //else return httpError(400, "$command is not a valid command for all switches specified")
       
}

//set pump delay
def setDelay(){
	//I'm sending deviceid and delay - still need to update to parse the 2 
    //   /delay/deviceid=' + deviceID + '&delay=' + value;
	log.debug "setdelay: " + params.command
    
    def p = params.command.split('&')
    def delay = p[1].split('=')[1]  
	
    state.delay = delay 
    if (delay == 'delete') state.delay = ''
     
    if (state.delay && state.delay == delay) {
    	log.debug "Delay set = " + delay
        return [error: false, data: 'delay set']
    }
    else if ( delay == 'delete' && !state.delay) {
    	log.debug "Delay deleted "
        return [error: false, data: 'delay deleted']
    }
    else return [error: true, data: 'delay not set']
    //else return httpError(400, "$command is not a valid command for all switches specified")
       
}

//set schedules times to run
def setScheduleTimes(){
	unschedule()
    log.debug "setting schedule times"
    def message = "";    
    state.scheduleMap.each{    
        def i = it.key
        def scheduleTime = state.scheduleMap[it.key]['start_time']        
        message += state.scheduleMap[it.key]['name']
        
        def hms = scheduleTime.split(':')  

        int hh = hms[0].toInteger()
        int mm = hms[1].toInteger()
        int ss = 0

        //set schedule run times            
        schedule("${ss} ${mm} ${hh} ? * *", "Check${i}")
        log.debug "set schedule Check${i} to ${hh}:${mm}"
        message += " set to ${hh}:${mm}, "
       
	}
    note("schedule", message, "d")
}

//command to recieve schedules
def setSchedule() {  

    log.debug(params.command)
    def sch = params.command.split('&')
    def sch_id = sch[0].split('=')[1]    
    
    boolean Sch_Set
    def count = 1
    if (sch[4].split('=')[1] == 'manual'){    	
    	state.manualMap = ['scheduleid' : sch[0].split('=')[1], 'start_time' : sch[1].split('=')[1], 'name' : sch[2].split('=')[1], 'type' : sch[4].split('=')[1]]        
        Sch_Set = true
       }
    else if (state.scheduleMap){
    	state.scheduleMap.each{        	
       		log.debug "key $it.key, count $count"
            if (count == it.key.toInteger()) count++            
            if (state.scheduleMap[it.key]['scheduleid'] == sch[0].split('=')[1]){
            	
                state.scheduleMap[it.key]['start_time'] = sch[1].split('=')[1]
                state.scheduleMap[it.key]['name'] = sch[2].split('=')[1]
                state.scheduleMap[it.key]['type'] = sch[4].split('=')[1]
            //add deviceid    
                log.debug "Schedule updated"
                Sch_Set = true
                return
			}
    	}
    }
    else {
    	state.scheduleMap = [:]
        state.scheduleMap[1] = ['scheduleid' : sch[0].split('=')[1], 'start_time' : sch[1].split('=')[1], 'name' : sch[2].split('=')[1], 'type' : sch[4].split('=')[1]]		
        Sch_Set = true
		log.debug "Schedule created"
    }
    if (!Sch_Set && count <= 6){
    	state.scheduleMap[count+1] = ['scheduleid' : sch[0].split('=')[1], 'start_time' : sch[1].split('=')[1], 'name' : sch[2].split('=')[1], 'type' : sch[4].split('=')[1]]        
        Sch_Set = true
        log.debug "Schedule added at $count"
        }
   	
    if (Sch_Set){
    	
        setScheduleTimes()    
		//return httpError(200, "schedule set")
        }
    else return httpError(200, "schedule declined, count exceeded 6") 
    
}

//remove schedule
def deleteSchedule() {  

    log.debug(params.command)
    def sch = params.command.split('&')
    def sch_id = sch[0].split('=')[1]
	log.debug(sch_id)
    def message = "";
    def count = 0
    boolean remove = false
        
    if (state.scheduleMap){
    	state.scheduleMap.each{        	
       		if (state.scheduleMap[it.key]['scheduleid'] == sch_id){            	           
            	count = it.key
                remove = true
                message += state.scheduleMap[it.key]['name']
                message += " removed"
                log.debug "Schedule removed"
                return
                }
    	}
    }
    log.debug count
    if (remove) {
    	state.scheduleMap.remove(count)        
        setScheduleTimes()
        note("schedule", message, "d")
	}
    else log.debug "nothing to delete"
    
}
    
//retrieve current runlist
def getTodaysTimes(sch){
    log.debug "get todays times for Check$sch"
    
    def respMessage = ""
    def result = []
    def key = state.key
    def delay = state.delay
    if (!state.delay) delay = 0
    
    def schedule_map
    def scheduleID
    schedule_map = state.scheduleMap[sch]
    if (sch == 'manual') schedule_map = state.manualMap    
    if (!schedule_map){
    	scheduleID = sch
        sch = 'manual'
        schedule_map = state.manualMap
        }
    else scheduleID = schedule_map['scheduleid']    
    def switchID = state.switches["${settings.switches[0]}"]
    
    state.active_sch = sch
    def update = true
    
    def schedule_name
    def run_length
    def scheduleTime
    def scheduleZone = '10:' + delay + ',' //  1,'
    def status_message
    def weather_message
    state.run_today = true    
           
    def newuri =  "http://api.spruceirrigation.com/v1/scheduler.php/runlist?deviceid="
        newuri += switchID
        newuri += "&scheduleid="
        newuri += scheduleID
        newuri += "&run=true"
        
	log.debug newuri
    
    def GETparams = [
        uri: newuri,		
        headers: [ 	"Authorization": key],           
    ]
    try{ httpGet(GETparams) { resp ->	
        //scheduleid
        schedule_name = resp.data['schedule_name']
        state.run_today = resp.data['run_today']
        scheduleTime = resp.data['start_time']
        scheduleZone += resp.data['runlist']
		run_length = resp.data['run_length']
        status_message = resp.data['status_message']
        weather_message = resp.data['rain_message']
    	}
	}
    catch (e) {        
        log.debug "send DB error: $e"
        update = false
        
    }
    
    respMessage += schedule_name
    result[0] = weather_message
    log.debug scheduleZone
    
    if (state.run_today){
    	result[0] = "active"
    	respMessage += " starts in 1 min\n "
    }
    else {
    	//result[0] = "skip"
    	respMessage += " skip today "
	}
    
    if (update){
    	     
        
        //save schedule time & zone settings
        if (sch == 'manual'){
        	state.manualMap['run_list'] = scheduleZone
            state.manualMap.put ('run_length', run_length)
            }
        
        else {
        	schedule_map.put ('name', schedule_name)
            state.scheduleMap[sch]['start_time'] = scheduleTime
            
            //only update if run today
            if (state.run_today){
            	state.scheduleMap[sch].put ('run_list', scheduleZone)
            	state.scheduleMap[sch].put ('run_length', run_length)                
			}

            def hms = scheduleTime.split(':')    

            int whh = 23
            int wmm = 0
            int wtime = 0

            int hh = hms[0].toInteger()
            int mm = hms[1].toInteger()
            int ss = 0

            if ( (hh*60 + mm) <= (whh * 60 + wmm) ){
                wtime = hh*60 + mm - 5
                whh = wtime / 60
                wmm = ((wtime / 60 * 100) - (whh * 100)) * 60 /100
                log.debug "set schedule to ${hh}:${mm}"
            }
            //set schedule run time
            schedule("${ss} ${mm} ${hh} ? * *", "Check${sch}")
		}        
        respMessage += status_message
    }
    else respMessage += " update failed"
    
    result[1] = respMessage
    return result
    //return respMessage
}

//parse zone times/cycles from runlist
def zoneCycles(sch) {    
    int i = 0
    
    if (state.run_today){
        def schedule_map = state.scheduleMap[sch]
        if (sch == 'manual') schedule_map = state.manualMap

        def scheduleID = schedule_map['scheduleid']    
        def zoneArray = schedule_map['run_list']
        log.debug zoneArray

        def newMap = zoneArray.split(',')

        state.cycleMap = [:]
        state.timeMap = [:]
        def option = []
    //need to add additional settings? added above at scheduleZone = '10:1,'  
        for(i = 0; i < 17; i++){    	
            option = newMap[i].toString().split(':')
            if (i == 0) state.timeMap."${i+1}" = option[0]
            else if (option[1] != "0") state.timeMap."${i+1}" = (Math.round(option[0].toInteger() / option[1].toInteger())).toString()
            else state.timeMap."${i+1}" = option[0]
            state.cycleMap."${i+1}" = option[1]
            }
        log.debug "cycleMap: ${state.cycleMap}"
        log.debug "timeMap: ${state.timeMap}"

        //send settings to controller
        settings.switches.settingsMap(state.cycleMap, 4001)
        runIn(20, sendTimeMap)
    }
}

//send runlist times to spruce controller
def sendTimeMap(){
	settings.switches.settingsMap(state.timeMap, 4002)
    }

def parse(description) {
	log.debug(description)
}

//***************** event handlers *******************************
//controller evts
def switchHandler(evt) {    
    log.debug "switchHandler: ${evt.device}, ${evt.name}, ${evt.value}"
        
    if (state.run_today && evt.value == 'off') cycleOff()
    else if (!state.run_today && evt.value == 'programOn') manual_schedule()
    
    //post zone on/off to web
    if (evt.value.contains('on') || evt.value.contains('off')){
        
        def device = state.switches["${evt.device}"]   

        def zonestate = 0
        if (evt.value.contains('on')) zonestate = 1    
        
        def EP = 0
        if (evt.name != "switch") EP = evt.name.replace('switch',"").toInteger()
        
        def scheduleMap = state.scheduleMap[state.active_sch]
        if (state.active_sch == 'manual') scheduleMap = state.manualMap
        
        def scheduleid = 0
        if (EP == 0) scheduleid = scheduleMap['scheduleid']
        else if (state.run_today) scheduleid = scheduleMap['scheduleid']
        
        def duration = scheduleMap['run_length']
        if (zonestate == 0 && EP != 0) duration = 0
        else if (!state.run_today) duration = settings.switches.currentValue('minutes')[0].toInteger() * 60     
        else if (EP != 0) duration = state.timeMap[(EP+1).toString()].toInteger() * 60        
       
        log.debug "Zone ${EP} ${zonestate} for ${duration}"

        def postTime = now() / 1000

        def POSTparams = [
                        uri: "http://api.spruceirrigation.com/v1/zone",
                        headers: [ 	"Authorization": state.key], 
                        body: [
                            zid: device,                        
                            zone: EP,
                            zonestate: zonestate,
                            zonetime: postTime.toInteger(),
                            duration: duration,
                            schedule_id: scheduleid
                        ]
                    ]

        sendPost(POSTparams)
    
    }
}

//sensor evts
def sensorHandler(evt) {
    log.debug "sensorHandler: ${evt.device}, ${evt.name}, ${evt.value}"
    
    def device = state.sensors["${evt.device}"]
    
    def uri = "http://api.spruceirrigation.com/v1/"
    if (evt.name == "humidity") uri += "moisture"    
    else uri += evt.name
    
    def value = evt.value
    
    def POSTparams = [
                    uri: uri,
                    headers: [ 	"Authorization": state.key], 
                    body: [
                        deviceid: device,
                        value: value                        
                    ]
                ]

	sendPost(POSTparams)
}

def sendPost(POSTparams) {
	try{
            httpPost(POSTparams) { log.debug 'Data pushed to DB' }
            } 
    catch (e) {
        log.debug 'send DB error: $e'
    }
}

//***************** schedule commands ******************************
//schedule on
def schOn(){	
    log.debug "run today: ${state.run_today}"
    if(state.run_today){
        settings.switches.zon()
        def schedule_map = state.scheduleMap[state.active_sch]
        if (state.active_sch == 'manual') schedule_map = state.manualMap

        def sch_name = schedule_map['name']
        def run_time = schedule_map['run_length']
        run_time = (run_time / 60).toDouble().round(1)
        String finishTime = new Date(now() + (schedule_map['run_length'] * 1000).toLong()).format('h:mm a', location.timeZone)
        note("active", "${sch_name} ends at ${finishTime}", "d")
    }
    else {
    	settings.switches.programOff()
    	//note("skip", "Skip Schedule", "d")
    }
}

def cycleOff(){
	log.debug "schedule finished"
    state.run_today = false
    //settings.switches.off()
    def schedule_map = state.scheduleMap[state.active_sch]
    if (state.active_sch == 'manual') schedule_map = state.manualMap
    def sch_name = schedule_map['name']
    
    String finishTime = new Date(now().toLong()).format('EE @ h:mm a', location.timeZone)
    note('finished', "${sch_name} finished at ${finishTime}", 'd')    
}

//**************** scheduled times ********************
def Check1(){	
    runIn(30, zoneCycles1)
    runIn(60, schOn)
    settings.switches.programWait()
    def result = getTodaysTimes('1')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
	log.debug "Starting Check 1 in 1 minute"    
}
def zoneCycles1() {zoneCycles('1')}

def Check2(){
	runIn(30, zoneCycles2)
    runIn(60, schOn)
    settings.switches.programWait()
    def result = getTodaysTimes('2')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
    log.debug "Starting Check 2 in 1 minute"  
}
def zoneCycles2() {zoneCycles('2')}

def Check3(){
	runIn(30, zoneCycles3)
    runIn(60, schOn)
    settings.switches.programWait()
    def result = getTodaysTimes('3')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
    log.debug "Starting Check 3 in 1 minute"  
}
def zoneCycles3() {zoneCycles('3')}

def Check4(){
	runIn(30, zoneCycles4)
    runIn(60, schOn)
    settings.switches.programWait()
    def result = getTodaysTimes('4')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
    log.debug "Starting Check 4 in 1 minute"  
}
def zoneCycles4() {zoneCycles('4')}

def Check5(){
	runIn(30, zoneCycles5)
    runIn(60, schOn)
    settings.switches.programWait()
	def result = getTodaysTimes('5')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
    log.debug "Starting Check 5 in 1 minute"  
}
def zoneCycles5() {zoneCycles('5')}

def Check6(){
	runIn(30, zoneCycles6)
    runIn(60, schOn)
    settings.switches.programWait()
    def result = getTodaysTimes('6')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
    log.debug "Starting Check 6 in 1 minute"  
}
def zoneCycles6() {zoneCycles('6')}

def manual_schedule(){
	log.debug "Manual Schedule starting"
    runIn(30, zoneCyclesM)
    runIn(60, schOn)
    
    def result = getTodaysTimes('manual')
	def status = result[0]
    def message = result[1]
    log.debug "$status, $message"
    note(status, message, "d")
    settings.switches.programWait()
    
    log.debug "Starting Check M in 1 minute"
}
def zoneCyclesM() {zoneCycles('manual')}


//************* notifications to device, pushed if requested ******************
def note(status, message, type){
	log.debug "${status}:  ${message}"
    settings.switches.notify("${status}", "${message}")
    if(notify)
    {
      if (notify.contains('Daily') && type == "d"){       
        sendPush "${message}"
      }
      if (notify.contains('Weather') && type == "f"){     
        sendPush "${message}"
      }
      if (notify.contains('Warnings') && type == "w"){     
        sendPush "${message}"
      }
      if (notify.contains('Moisture') && type == "m"){        
        sendPush "${message}"
      }      
    }
}

//**************************** device commands - not used**************************************
def runZone(){
	log.debug(params.command)
    // use the built-in request object to get the command parameter
    def command = params.command
    def zoneonoff = command.split(',')
    
    switch(zoneonoff[0]) {
        case "zon":
            //set turn on time
            def runT = zoneonoff[2].toInteger() / 60
    		settings.switches.manualTime(runT)
         	//pumpOn()
            zoneOn(zoneonoff[1])
            return [error: false, return_value: 1]
            //def response = 'callback({"error":false,"return_value":1})'
            //render contentType: "application/jsonp", data: response, status: 200
            break
        case "zoff":
            zoneOff(zoneonoff[1])
            //
            //def response = 'callback({"error":false,"return_value":1})'
            //render contentType: "application/jsonp", data: response, status: 200
            return [error: false, return_value: 1]
            break
        case "son":        	
            //use manual table to store run values
            runIn(30, zoneCyclesM)
            runIn(60, schOn)
            //send scheduleID
            def result = getTodaysTimes(zoneonoff[1])
            def status = result[0]
            def message = result[1]

            log.debug "$status, $message"
            note(status, message, "d")
            settings.switches.programWait()

            log.debug "Starting Check M in 1 minute"
            return [error: false, return_value: 1]
            break
        case "soff":            
         	cycleOff()

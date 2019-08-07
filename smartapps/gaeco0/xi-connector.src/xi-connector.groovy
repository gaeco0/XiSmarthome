/**
 *  Xi Smarthome
 *
 *  Copyright 2019 주용은
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
import groovy.transform.Field


definition(
    name: "Xi Smarthome",
    namespace: "gaeco0",
    author: "주용은",
    description: "\uC790\uC774 \uC2A4\uB9C8\uD2B8\uD648 \uC5F0\uB3D9",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/gaeco0/XiSmarthome/master/icons/xi_1x.png?token=AB2YHHETEU3OLURUVU2MA5K5IKERE",
    iconX2Url: "https://raw.githubusercontent.com/gaeco0/XiSmarthome/master/icons/xi_2x.png?token=AB2YHHAVM2GFY637LIGZNL25IKEVI",
    iconX3Url: "https://raw.githubusercontent.com/gaeco0/XiSmarthome/master/icons/xi_2x.png?token=AB2YHHAVM2GFY637LIGZNL25IKEVI") {
    
}
preferences {
	page(name: "mainPage")
    page(name: "findAptNamePage")
    page(name: "findAptResultPage")
    page(name: "authenticationPage")
    page(name: "authenticationResultPage")
}
/*
해야할 것
 - 암호 변경
 - 로그인 후 디바이스 리스트 세팅
 - 디바이스 리스트에서 디바이스 선택하여 설치
 - DTH 작성(스위치, Stateless Button(푸쉬버튼))
*/
@Field
COOKIE = ""

@Field
BASE_URL="https://m.ezville.net"

@Field
APT_LIST = []

def mainPage() {
	dynamicPage(name: "mainPage", title: "Xi Smart Home Settings", nextPage: null, uninstall: true, install: true) {
    	section("Xi 설정") {
        	href "findAptNamePage", title: "아파트 검색", description:"${settings.aptTag}"
        }
        section("설정 가이드") {
        	paragraph "아파트 검색을 눌러 아파트 이름을 검색/선택한 후"
            paragraph "동 호수를 입력하고 전송을 눌러 인증코드를 전송합니다."
            paragraph "월패드에 뜨는 공지사항 알림에 뜨는 인증 코드를 입력하고 저장을 누릅니다."
        }
    }
}
def findAptNamePage() {
	//아파트 검색
    dynamicPage(name:"findAptNamePage", title:"아파트 검색", nextPage: "findAptResultPage") {
    	section() {
        	input "aptSrchNm", "string", title:"아파트 이름"
        }
    }
}
def findAptResultPage() {
	findAptByName()
	dynamicPage(name: "findAptResultPage", title: "아파트 검색 결과", nextPage: "authenticationPage") {
    	section() {
        	input "aptTag", "enum", title: "아파트 선택", options:APT_LIST, multiple: false, required: true
            input "dong", "number", title: "동", required: true
            input "ho", "number", title: "호", required: true
        }
        section() {
        }
    }
}
def setHkey() {
	def tmp = settings.aptTag.split(" \\[ ")
    def tmp2 = tmp[1].split(" \\]")
    def hkey = tmp2[0]
    settings.hkey = hkey
}
def setDeviceId() {
	def params = [
		"uri":"https://api.ipify.org/",
        "contentType":"text/plain"
    ]
    httpGet(params) {resp -> 
    	app.updateSetting(deviceId, resp.data)
    }
}
def requestWallpadAuthCode() {
	// GET cookie & Send Request
    def params = [
    	"uri": "${BASE_URL}/ezvillehn/mobile2/info/wallpadAuthCall.php",
        "requestContentType": "application/x-www-form-urlencoded; charset=UTF-8",
        "contentType": "text/xml; charset=UTF-8",
        "headers": [
        	"Cookie":"${COOKIE}"
        ],
        "query": [
            "hkey":"${settings.hkey}",
            "uid":"${settings.dong}-${settings.ho}",
            "devieid":"${settings.deviceId}",
            "boo":"1"
		]
    ]
    log.debug params
    httpPost(params) {resp ->
    	if(resp.data.authcertifyinfo.certifyyn == "Y") {
        } else {
        	log.error "에러발생 ${resp.data.authcertify.authcertifyinfo.failreason}"
        }
    }
}
def setPassword() {
	if(settings.pwd == settings.pwdCfrm) {
    	def params = [
            "uri": "${BASE_URL}/ezvillehn/mobile2/info/loginPwdSet.php",
            "requestContentType": "application/x-www-form-urlencoded; charset=UTF-8",
            "contentType": "text/xml; charset=UTF-8",
            "headers": [
                "Cookie":"${COOKIE}"
            ],
            "query": [
                "hkey":"${settings.hkey}",
                "uid":"${settings.dong}-${settings.ho}",
                "pwd": "${settings.password}",
                "devicetoken": "PAP91",
                "devieid":"${settings.deviceId}",
                "devicekind": "android",
                "boo":"1"
            ]
        ]
        //TODO:패스워드 등록 및 리턴
    } else {
    	//TODO: 오류 발생
    }
}
def wallPathAuth() {
	def params = [
    	"uri": "${BASE_URL}/ezvillehn/mobile2/info/wallpadAuthCertify.php",
        "requestContentType": "application/x-www-form-urlencoded; charset=UTF-8",
        "contentType": "text/xml; charset=UTF-8",
        "headers": [
        	"Cookie":"${COOKIE}"
        ],
        "query": [
            "hkey":"${settings.hkey}",
            "uid":"${settings.dong}-${settings.ho}",
            "devieid":"${settings.deviceId}",
            "authKeyNum":"${settings.authCode}",
            "boo":"1"
		]
    ]
	httpPost(params) {resp ->
    	if(resp.data.authcertifyinfo.certifyyn != "Y") {
        	//인증 실패
            def err = (resp.data.authcertifyinfo.failreason == "authKeyNumNonEqual"?"인증번호가 틀렸습니다.":"인증실패")
            log.error err
            return err
        } else {
        	return "SUCCESS"
        }
    }
}
def authenticationResultPage() {
    def result = wallPathAuth()
    if(result == "SUCCESS") {
    	//Display password page
        dynamicPage(name:"authenticationResultPage", title:"암호 등록", nextPage:"mainPage") {
        	section() {
            	paragraph "스마트홈 로그인 용 비밀번호를 등록합니다."
                paragraph "기존 등록하신 계정은 그대로 유지됩니다."
                paragraph "해당 암호는 Smartthings에서만 사용되므로 기존 어플리케이션에는 영향이 없습니다."
            	input "pwd", "password", title: "암호", required:true
                input "pwdCfrm", "password", title: "암호 확인", required:true
            }
        }
    } else {
    	// display result
        //result
        dynamicPage(name:"authenticationResultPage", title:"에러 발생", nextPage:"authenticationPage") {
        	section() {
            	paragraph "${result}"
            }
        }
    }
}
def authenticationPage() {
	//Check Valid
    if(settings.aptTag != null && settings.dong != null && settings.ho != null) {
    	//월패드 인증번호 전송
        setHkey()
        setDeviceId()
        requestWallpadAuthCode()
    	dynamicPage(name: "authenticationPage", title: "월패드 인증", nextPage: "authenticationResultPage") {
            section() {
            	input "authCode", "number", title: "월패드 인증번호를 입력하세요", required: true
            }
        }
    } else {
    	dynamicPage(name: "authenticationPage", title: "", nextPath: "findAptResultPage") {
        	section() {
            	paragraph "입력값이 올바르지 않습니다."
            }
        }
    }
}
def findAptByName() {
	//아파트 검색 Http Request
    def params = [
    	"uri": "${BASE_URL}/ezvillehn/mobile2/info/houseBlockList.php",
        "requestContentType": "application/x-www-form-urlencoded; charset=UTF-8",
        "contentType": "text/xml; charset=UTF-8",
        "query": [
        	"hname":"${settings.aptSrchNm}",
            "ckind":"xi"
		]
    ]
    try {
    	httpPost(params) { resp ->
            resp.headers.each { header -> 
            	if("Set-Cookie".equals(header.name)) {
                	COOKIE = header.value
                }
            }
            APT_LIST.clear()
            resp.data.houseblock.each { houseblock ->
            	APT_LIST.push("${houseblock.hname} [ ${houseblock.hkey} ]")
            }
        }
    } catch(err) {
    	log.error(err)
    }
}
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	//
}

// TODO: implement event handlers
package com.android.contacts.location;

public class PhoneNumUtils {
	
    static final String[] KNOW_PREFIX = new String[] {
    	"+86",
    	"0086",
    	"106", //小灵通发端消息前缀
    	"12520", //移动飞信,
    	"17951",//移动ip拨号
    	"17909",//电信ip拨号
    	"12593",
    	"17911"
    };
	
	public static  boolean isInternationPhone(String phoneNum)
    {
		if(phoneNum.length() < 1)
			return false;
    	//如果任何电话号码以+开头,并且不是+86和+0086,则是国际电话
    	if(phoneNum.charAt(0) == '+'){
    		if(phoneNum.startsWith("+86") || phoneNum.startsWith("+0086")){
    			return false; // This is Chinese phone
    		}
    		return true;
    	}
    	
    	//如果任何电话以00开头,并且不是0086,则是国际电话
    	if(phoneNum.startsWith("00")){
    		if(phoneNum.startsWith("0086")){
    			return false;
    		}
    		return true;
    	}
    	
    	//其他任何电话,都认为是国内电话
    	return false;
    }
    
	public static String removePlus(String phoneNum){
		if(phoneNum.length() < 1)
			return phoneNum;
		
		if(phoneNum.charAt(0) == '+') //remove plus mark first
			phoneNum = phoneNum.substring(1);
		
		return phoneNum;
    }
	
	public static String removeKnowPrefix(String phoneNum){
        phoneNum = phoneNum.replace("-","");
    	for(int i = 0; i < KNOW_PREFIX.length;i++){
    		if(phoneNum.startsWith(KNOW_PREFIX[i])){
    			phoneNum = phoneNum.substring(KNOW_PREFIX[i].length());
    			return phoneNum;
    		}
    	}
    	
		return phoneNum;
    }
	
	public static boolean isFixedPhoneWithoutCityCode(String phoneNum)
	{
		if(phoneNum.length() == 7 || phoneNum.length() == 8)
			return true;
		
		return false;
	}
	
	public static String getChineseLocationKey(String phoneNum){
		String key;
		
		if(phoneNum.length() < 3)
			return null;
		
		if(phoneNum.charAt(0) == '0' ){
			if (phoneNum.charAt(1) == '1' || phoneNum.charAt(1) == '2'){
				key = phoneNum.substring(0, 3);
			}
			else {
				if(phoneNum.length() < 4) 
					return null;
				key = phoneNum.substring(0,4);
			}			
		}
		else{
			if(phoneNum.length() < 7)
				return null;
			
			key = phoneNum.substring(0, 7);
		}
		
		return key;
	}
    
}

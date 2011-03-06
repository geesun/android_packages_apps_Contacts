package com.android.contacts.location;

import com.android.phone.location.PhoneLocation;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class PhoneNumProcess {
	private Context m_context;
	private String m_phoneNum;
	private String m_phoneLocation;
	private String m_phoneLocationCode;
	private String m_myLocationCode;
	private boolean m_isIpDailerEnable; 
	private String m_ipDailerPrefix;
	
	public PhoneNumProcess(Context ctx, String num){
		m_context = ctx;

		LocationPreference  myPrefs = new LocationPreference(ctx);		
		m_myLocationCode = myPrefs.getMyLocationCode();
		m_ipDailerPrefix = myPrefs.getIpDailerPrefix();
		m_isIpDailerEnable = myPrefs.isIpDailerOn();
		
        if(!m_isIpDailerEnable){
            m_phoneNum = num;
            m_phoneLocation = PhoneLocation.getCityFromPhone(m_phoneNum);
            return;
        }
		//如果提供的信息不够多,则认为是自己所在地
		if(num == null || num.length() <= 3){
			m_phoneNum = num;
			m_phoneLocationCode = " ";
			m_phoneLocation = " ";
			return ;
		}else{		
            num = num.replace("-","");
			//如果电话已经有IP拨号前缀,则删除拨号前缀,这样可以当成普通号码进行后续处理
			if(num.startsWith(m_ipDailerPrefix)){
				m_phoneNum = num.substring(m_ipDailerPrefix.length());
			}
			else
				m_phoneNum = num;
		}

		m_phoneLocation = PhoneLocation.getCityFromPhone(m_phoneNum);

        //如果没有设置自己的号码,或则自己的号码没有识别出来
        if(m_myLocationCode.equals(" ")){
            m_phoneLocationCode = " ";
            return;
        }

		//LocationInfo location = new LocationInfo(ctx,m_phoneNum);
		m_phoneLocationCode = PhoneLocation.getCodeFromPhone(m_phoneNum);
		
		if(m_phoneLocationCode == null || m_phoneLocation == null){
			m_phoneLocation = " ";
			m_phoneLocationCode = " ";
			
			//如果是固定电话没有加区号,则显示自己所在城市
			if(!PhoneNumUtils.isInternationPhone(m_phoneNum)
					&& PhoneNumUtils.isFixedPhoneWithoutCityCode(m_phoneNum)){			
				m_phoneLocation = myPrefs.getMyLocation();
				m_phoneLocationCode = m_myLocationCode;
				//如果开启智能拨号,则把电话号码前加上区号
				if(m_isIpDailerEnable){				
					m_phoneNum = m_myLocationCode + m_phoneNum;
				}
			}
		}
        

	}
	
	public String getPhoneLocation()
	{
		return m_phoneLocation;
	}
	 
	public String getPhoneWithIp(){
		//如果IP拨号没有开启,或者位置信息没有找到,则直接返回原始号码
		if(m_isIpDailerEnable && !m_phoneLocationCode.equals(" ")){
			//如果是特殊号码,如95528,或者国际号码,或者本定号码,则返回原始号码
			if(m_phoneLocationCode.equals("001") || m_phoneLocationCode.equals("002") 
					||m_phoneLocationCode.equals(m_myLocationCode))
				return m_phoneNum;
			
    		String phone = PhoneNumUtils.removeKnowPrefix(m_phoneNum);
    		phone = PhoneNumUtils.removePlus(phone);
    		
			return m_ipDailerPrefix + phone;
		}
		return  m_phoneNum;
	}

	public void displayPhoneLocation()
	{
        /*
        if(!m_phoneLocation.equals(" ")){
            Toast toast = Toast.makeText(m_context, m_phoneLocation, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 50);
            toast.show();
        }
        */
	}
	
	
	
}

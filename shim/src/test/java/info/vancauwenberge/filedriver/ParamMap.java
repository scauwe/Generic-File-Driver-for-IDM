package info.vancauwenberge.filedriver;

import java.util.HashMap;

import com.novell.nds.dirxml.driver.xds.DataType;
import com.novell.nds.dirxml.driver.xds.Parameter;

@SuppressWarnings("serial")
public class ParamMap extends HashMap<String, Parameter> {
	public void putParameter(final String name, final String value){
		this.put(name, new Parameter(name,value,DataType.STRING));
	}

	public void putParameter(final String name, final long value){
		//No clue how the Parameter works, so just overwrite what we need...
		this.put(name, new Parameter(name,"0",DataType.LONG){
			@Override
			public Long toLong(){
				return value;
			}
		});
	}

	public void putParameter(final String name, final int value){
		//No clue how the Parameter works, so just overwrite what we need...
		this.put(name, new Parameter(name,"0",DataType.INT){
			@Override
			public Integer toInteger(){
				return value;
			}
		});
	}

	public void putParameter(final String name, final boolean value){
		this.put(name, new Parameter(name,"true",DataType.BOOLEAN){
			@Override
			public Boolean toBoolean(){
				return value;
			}
		});
	}
}

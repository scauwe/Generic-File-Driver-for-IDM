package info.vancauwenberge.filedriver;

import com.novell.nds.dirxml.driver.TraceInterface;
import com.novell.nds.dirxml.driver.XmlDocument;

public class SysoutTrace implements TraceInterface{

	@Override
	public void trace(final int level, final String paramString) {
		System.out.println(paramString);		
	}

	@Override
	public void trace(final int level, final XmlDocument paramXmlDocument) {
		System.out.println(paramXmlDocument);
	}

	@Override
	public int getLevel() {
		return 99;
	}

}

package test.info.vancauwenberge.filedriver;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.novell.nds.dirxml.driver.Trace;


@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractStrategyTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Trace.registerImpl(SysoutTrace.class, 0);

	}

	public Trace getTrace(){
		return new Trace(">");
	}
}

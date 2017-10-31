package info.vancauwenberge.filedriver.api;

import java.io.File;

public interface IPostProcessStrategy  extends ISubscriberStrategy{

	void doPostProcess(File result);

}

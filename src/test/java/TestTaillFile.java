import java.io.File;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.commons.io.FileUtils;
import org.apache.flume.serialization.TransferStateFileMeta;

import com.github.ningg.flume.source.MyDurablePositionTracker;

public class TestTaillFile {
	public static void main(String[] args) throws IOException {
//	System.out.println(DeletePolicy.MD5.name());
//	File file = new File("/home/song/temp/tttt111");
//	
//	FileUtils.write(file, null);
//}
//
//	static enum DeletePolicy {
//		NEVER, IMMEDIATE, DELAY, MD5
		
		
		File trackerFile = new File("/home/song/workspace/bigdata/firstMavenTest/logs/.flumespooltail/.flumespooltailfile-main.meta");
		String target = "/home/song/workspace/bigdata/firstMavenTest/logs/info.log";
		MyDurablePositionTracker oldTracker = new MyDurablePositionTracker(trackerFile, target);
		System.out.println(oldTracker.getTarget());
		System.out.println(oldTracker.getPosition());
		
//		oldTracker.storePosition(123);
//		
//		System.out.println(oldTracker.getPosition());
		
		oldTracker.close();
		
	}
}

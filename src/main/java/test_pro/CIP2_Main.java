package test_pro;

import base.Tag;
import base.TagListGenerator;
import base.TagRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocals.IdentifyTool;
import utils.Environment;
import utils.Reader_M;
import utils.Recorder;

import java.util.List;

/**
 * @author Kirin Huang
 * @date 2022/8/12 下午4:53
 */
public class CIP2_Main {
    public static void main(String[] args) {
        int instanceNumber = 1;
        int allTagNumber = 40000;
        int tagPerCid = 100;
        int unknownTagNumber = 0;
        int expectedTagNum = allTagNumber - unknownTagNumber;
        int missingTagNumber = (int)Math.ceil(expectedTagNum * 0.0);
        int tagIDLength = 14;
        int categoryIDLength = 32;
        Logger logger = LogManager.getLogger(CIP2_Main.class);

        Recorder recorder = new Recorder();

        for (int r = 0; r < instanceNumber; r++){
            logger.error("<<<<<<<<<<<<<<<<<<<< 模拟次数: " + r + ">>>>>>>>>>>>>>>>>>>");

            TagRepository tagRepository = TagListGenerator.generateTagRepository(tagIDLength, categoryIDLength, allTagNumber, tagPerCid,unknownTagNumber, missingTagNumber);
            List<Tag> allTagList = tagRepository.getAllTagList();
            List<Tag> expectedTagList = tagRepository.getExpectedTagList();
            List<Tag> tagList = tagRepository.getActucaltagList();


            Environment environment = new Environment(allTagList, expectedTagList, tagList,expectedTagNum/tagPerCid);

            environment.createType1(4000, 1600, 1, 1);


//            IdentifyTool ecip = new ECIP2(logger,recorder,environment);
//            ecip.execute();
//            environment.reset();
//            for(Reader_M reader_m : environment.getReaderList()) {
//                reader_m.recorder = new Recorder();
//            }
            IdentifyTool cls = new ECLS2(logger,recorder,environment);
            cls.execute();
            environment.reset();
            for(Reader_M reader_m : environment.getReaderList()) {
                reader_m.recorder = new Recorder();
            }
            IdentifyTool dls = new EDLS2(logger,recorder,environment);
            dls.execute();
            environment.reset();
            for(Reader_M reader_m : environment.getReaderList()) {
                reader_m.recorder = new Recorder();
            }


        }
    }
}

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
public class EDLS_Main {
    public static void main(String[] args) {
        int instanceNumber = 1;
        int allTagNumber = 22500;
        int unknownTagNumber = 0;
        int expectedTagNum = allTagNumber - unknownTagNumber;
        int missingTagNumber = 0;
        int tagIDLength = 14;
        int categoryIDLength = 32;
        Logger logger = LogManager.getLogger(EDLS_Main.class);

        logger.error("Total number of tags: [" + allTagNumber + "]");
        logger.error("Total number of expected tags: [" + (allTagNumber - unknownTagNumber) + "]");
        logger.error("Actual number of tags: [" + (allTagNumber - missingTagNumber - unknownTagNumber) + "]");

        Recorder recorder = new Recorder();

        for (int r = 0; r < instanceNumber; r++){
            logger.error("<<<<<<<<<<<<<<<<<<<< Instance: " + r + ">>>>>>>>>>>>>>>>>>>");

            TagRepository tagRepository = TagListGenerator.generateTagRepository(tagIDLength, categoryIDLength, allTagNumber, 100,unknownTagNumber, missingTagNumber);
            List<Tag> allTagList = tagRepository.getAllTagList();
            List<Tag> expectedTagList = tagRepository.getExpectedTagList();
            List<Tag> tagList = tagRepository.getActucaltagList();


            //Single Reader and Multi reader codes are almost same, we only give one reader for the environment
            Environment environment = new Environment(allTagList, expectedTagList, tagList,expectedTagNum/100);

            // single reader
            environment.createType1(4000, 1600, 1, 1);

            IdentifyTool edls01 = new EDLS(logger,recorder,environment);
            edls01.execute();
            environment.reset();
            for(Reader_M reader_m : environment.getReaderList()) {
                reader_m.recorder = new Recorder();
            }
            IdentifyTool edls001 = new EDLS4(logger,recorder,environment);
            edls001.execute();
            environment.reset();
            for(Reader_M reader_m : environment.getReaderList()) {
                reader_m.recorder = new Recorder();
            }
            IdentifyTool edls0001 = new EDLS5(logger,recorder,environment);
            edls0001.execute();
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

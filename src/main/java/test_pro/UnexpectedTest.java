package test_pro;

import base.Tag;
import base.TagListGenerator;
import base.TagRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocals.IdentifyTool;
import utils.Environment;
import utils.Recorder;

import java.util.List;

/**
 * @author Kirin Huang
 * @date 2022/8/12 下午4:53
 */
public class UnexpectedTest {
    public static void main(String[] args) {
        int instanceNumber = 1;
        int allTagNumber = 10000;
        int unknownTagNumber = 5000;
        int expectedTagNum = allTagNumber - unknownTagNumber;
        int missingTagNumber = (int)(expectedTagNum*0.8);
        int tagPerCid = 10;
        int tagIDLength = 96;
        int categoryIDLength = 32;
        Logger logger = LogManager.getLogger(UnexpectedTest.class);

        logger.error("Total number of tags: [" + allTagNumber + "]");
        logger.error("Total number of expected tags: [" + (allTagNumber - unknownTagNumber) + "]");
        logger.error("Actual number of tags: [" + (allTagNumber - missingTagNumber - unknownTagNumber) + "]");

        Recorder recorder = new Recorder();

        for (int r = 0; r < instanceNumber; r++){
            logger.error("<<<<<<<<<<<<<<<<<<<< Instance: " + r + ">>>>>>>>>>>>>>>>>>>");

            TagRepository tagRepository = TagListGenerator.generateTagRepository(tagIDLength, categoryIDLength, allTagNumber, tagPerCid,unknownTagNumber, missingTagNumber);
            List<Tag> allTagList = tagRepository.getAllTagList();
            List<Tag> expectedTagList = tagRepository.getExpectedTagList();
            List<Tag> tagList = tagRepository.getActucaltagList();


            //Single Reader and Multi reader codes are almost same, we only give one reader for the environment
            Environment environment = new Environment(allTagList, expectedTagList, tagList,expectedTagNum/tagPerCid);

            // single reader
            environment.createType1(4000, 1600, 1, 1);
            IdentifyTool edls = new EDLS5(logger,recorder,environment);
            edls.execute();
            // 不做意外标签处理
            IdentifyTool edls3 = new EDLS3(logger,recorder,environment);
            edls3.execute();

            // p=0.01
            environment.getAllTagList().forEach(tag -> tag.setActive(true));
            environment.getReaderList().forEach(reader_m -> reader_m.recorder = new Recorder());
            IdentifyTool edls2 = new EDLS(logger,recorder,environment);
            edls2.execute();

            // p = 0.001
            environment.getAllTagList().forEach(tag -> tag.setActive(true));
            environment.getReaderList().forEach(reader_m -> reader_m.recorder = new Recorder());
            IdentifyTool edls4 = new EDLS4(logger,recorder,environment);
            edls4.execute();

            // p = 0.0001
            environment.getAllTagList().forEach(tag -> tag.setActive(true));
            environment.getReaderList().forEach(reader_m -> reader_m.recorder = new Recorder());
            IdentifyTool edls5 = new EDLS5(logger,recorder,environment);
            edls5.execute();

        }
    }
}

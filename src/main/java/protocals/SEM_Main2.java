package protocals;

import base.Tag;
import base.TagListGenerator;
import base.TagRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.Environment;
import utils.Recorder;

import java.util.List;

/**
 * @author Kirin Huang
 * @date 2022/8/12 下午4:53
 */
public class SEM_Main2 {
    public static void main(String[] args) throws InterruptedException {
        int instanceNumber = 1;
        int allTagNumber = 110;
        int unknownTagNumber = 0;
        int expectedTagNum = allTagNumber - unknownTagNumber;
        int missingTagNumber = 0;
        int tagIDLength = 96;
        int categoryIDLength = 32;
        int density = 110;

        int allTagNumber2 = 100;
        int unknownTagNumber2 = 0;
        int expectedTagNum2 = allTagNumber - unknownTagNumber;
        int missingTagNumber2 = 0;
        int tagIDLength2 = 96;
        int categoryIDLength2 = 32;

        int density2 = 100;
        int nx = density2;
        int ny = density;

        Logger logger = LogManager.getLogger(SEM_Main2.class);

        logger.error("Total number of tags: [" + (allTagNumber+allTagNumber2) + "]");
        logger.error("Total number of expected tags: [" + (allTagNumber - unknownTagNumber+allTagNumber2-unknownTagNumber2) + "]");
        logger.error("Actual number of tags: [" + (allTagNumber - missingTagNumber - unknownTagNumber+allTagNumber2-missingTagNumber2-unknownTagNumber2) + "]");

        Recorder recorder = new Recorder();

        for (int r = 0; r < instanceNumber; r++){
            logger.error("<<<<<<<<<<<<<<<<<<<< Instance: " + r + ">>>>>>>>>>>>>>>>>>>");

            TagRepository tagRepository = TagListGenerator.generateTagRepository(tagIDLength, categoryIDLength, allTagNumber, density,unknownTagNumber, missingTagNumber);
            List<Tag> allTagList = tagRepository.getAllTagList();
            List<Tag> expectedTagList = tagRepository.getExpectedTagList();
            List<Tag> tagList = tagRepository.getActucaltagList();
            Thread.sleep(5);
            TagRepository tagRepository2 = TagListGenerator.generateTagRepository(tagIDLength2, categoryIDLength2, allTagNumber2, density2,unknownTagNumber2, missingTagNumber2);
            List<Tag> allTagList2 = tagRepository2.getAllTagList();
            List<Tag> expectedTagList2 = tagRepository2.getExpectedTagList();
            List<Tag> tagList2 = tagRepository2.getActucaltagList();

            allTagList.addAll(allTagList2);
            expectedTagList.addAll(expectedTagList2);
            tagList.addAll(tagList2);

            Environment environment = new Environment(allTagList, expectedTagList, tagList,expectedTagNum/density,nx,ny);

            environment.createType1(4000, 1600, 1, 1);

            SEM sem = new SEM(logger,recorder,environment);
            sem.identify();
//            sem.optimizeParams();
        }
    }
}

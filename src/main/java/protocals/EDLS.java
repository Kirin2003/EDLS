package protocals;

import LoF_Count.InitPseudoByCate;
import LoF_Count.LoF;
import LoF_Count.MultiHashLoF;
import LoF_Count.MultisplittingLoF;
import base.Tag;
import org.apache.logging.log4j.Logger;
import protocals.IdentifyTool;
import utils.*;

import java.util.*;

/**
 * @author Kirin Huang
 * @date 2022/8/8 下午10:19
 */
public class EDLS extends IdentifyTool {
    /**
     * 哈希函数的个数, 用于意外标签去除阶段
     */
    int numberOfHashFunctions = 1;
    /**
     * 假阳性误报率, 即意外标签通过成员检查的比率
     */
    double falsePositiveRatio = 0.01;


    /**
     * EDLS构造函数
     * @param logger 记录算法运行中的信息, 便于调试
     * @param recorder 记录器, 记录算法输出结果
     * @param environment 环境,里面有标签的数目,标签id和类别id列表,位置等信息和阅读器的数目,位置等信息
     */
    public EDLS(Logger logger, Recorder recorder, Environment environment) {
        super(logger, recorder, environment);
    }

    /**
     * EDLS算法执行入口
     * 多阅读器场景
     * 第一阶段, 所有阅读器同时工作, 去除意外标签, 等待所有阅读器工作完毕再进行下一阶段, 这样意外标签去除的多, 对下一阶段干扰的就少
     * 第二阶段, 所有阅读器同时工作, 识别存在标签, 所有阅读器工作完毕后, 所有阅读器识别的存在标签之和是存在标签, 不再存在标签中的是缺失标签
     */
    @Override
    public void execute(){
        int expectedActualCidNum = 0;
        Set<String> expectedActualCidSet = new HashSet<>();
        environment.getActualTagList().forEach(tag -> expectedActualCidSet.add(tag.getCategoryID()));
        expectedActualCidNum = expectedActualCidSet.size();

        Set<String> expectedCidSet = new HashSet<>();
        environment.getExpectedTagList().forEach(tag -> expectedCidSet.add(tag.getCategoryID()));
        int expectedCidNum = expectedCidSet.size();
        logger.debug("环境中的期望类别数=[ "+expectedCidNum+" ] 环境中真实存在的类别数=[ "+expectedActualCidNum+" ]");

        List<Reader_M> readers = environment.getReaderList();

        /**
         * 第一阶段, 意外标签去除阶段
         */
        unexpectedTagElimination();

        // 第一阶段的时间: 所有阅读器的执行时间中最长的作为第一阶段的时间
        double maxTime = 0;
        for(Reader_M reader_m : readers) {
            double t1 = reader_m.recorder.totalExecutionTime;
            if(t1 > maxTime) {
                maxTime = t1;
            }
        }
        recorder.totalExecutionTime = maxTime;
        logger.error("第一阶段结束, 所有阅读器的总时间:[ "+maxTime+" ]ms");
        double finalMaxTime = maxTime;
        readers.forEach(reader_m -> reader_m.recorder.totalExecutionTime = finalMaxTime);

        // 所有阅读器去除(去重)的意外标签的并集是总共去除的意外标签
        Set<Tag> eliminated = new HashSet<>();
        readers.forEach(reader_m -> eliminated.addAll(reader_m.recorder.eliminateTagList));
        recorder.eliminateTagList.addAll(eliminated);
        recorder.eliminationTagNum=recorder.eliminateTagList.size();

        System.out.println("去除的意外标签数:"+recorder.eliminateTagList.size());

        /**
         * 第二阶段, 识别存在的类别和缺失的类别的阶段
         */
        List<Tag> actuallist = environment.getActualTagList();

        InitPseudoByCate.initPseudoRandomListByCate(actuallist, 100, 15);

        LoF.estimate(environment.getActualTagList());
        int res1 = MultiHashLoF.estimate(environment.getActualTagList());
        int res2 = MultisplittingLoF.estimate(actuallist,0.0);
        System.out.println("multi hash:"+res2);
//        identify();
////
//        // 第二阶段所有阅读器的执行时间中最长的作为第二阶段的时间
//        double maxTime2 = 0;
//        for(Reader_M reader_m : readers) {
//            double t1 = reader_m.recorder.totalExecutionTime;
//            if(t1 > maxTime2) {
//                maxTime2 = t1;
//            }
//        }
//        recorder.totalExecutionTime = maxTime2;
//        logger.error("第二阶段结束, 所有阅读器的总时间:[ "+maxTime2+" ]ms");
//
//        readers.forEach(reader_m -> recorder.actualCids.addAll(reader_m.recorder.actualCids));
//
//        environment.getExpectedTagList().forEach(tag -> {
//            String cid = tag.getCategoryID();
//            if(!recorder.actualCids.contains(cid)){
//                recorder.missingCids.add(cid);
//            }
//        });
//        recorder.correctRate = 1-(expectedCidNum-expectedActualCidNum-recorder.missingCids.size())*1.0/(expectedCidNum);
//
//        System.out.println(" ");
//        Recorder recorder1 = environment.getReaderList().get(0).recorder;
//        System.out.println("总时间:"+recorder.totalExecutionTime);
//        System.out.println("缺失率列表:"+recorder1.missingRateList);
//        System.out.println("执行时间列表:"+recorder1.executionTimeList);
//        System.out.println("识别类别列表:"+recorder1.recognizedCidNumList);
//        System.out.println("识别存在类别列表:"+recorder1.recognizedActualCidNumList);
//        System.out.println("识别缺失类别列表:"+recorder1.recognizedMissingCidNumList);
//        System.out.println("误判标签数:"+(expectedCidNum-expectedActualCidNum-recorder.missingCids.size())+"准确率:"+recorder.correctRate);
//        System.out.println("去除的意外标签数:"+recorder.eliminateTagList.size());
    }

    /**
     * 第一阶段, 意外标签去除阶段, 使用布隆过滤器
     */
    public void unexpectedTagElimination() {
        UnexpectedTagEliminationMethod.BloomFilterMethod(numberOfHashFunctions, falsePositiveRatio,environment,logger);

    }

    /**
     * 第二阶段, 识别类别阶段(多阅读器)
     */
    public void identify() {
        for (Reader_M reader : environment.getReaderList()) {
            logger.error("<<<<<<<<<<<<<<<<<<<< 阅读器: " + reader.getID() + " >>>>>>>>>>>>>>>>>>>");
            // 每一次需要reset(),将环境中所有预期标签设为活跃, 因为有些标签在其他阅读器中识别为缺失, 但在某个阅读器中识别为存在, 这个标签是存在的
            environment.getExpectedTagList().forEach(tag -> tag.setActive(true));
            identify( reader);
        }
    }

    /**
     * 第二阶段, 识别类别阶段(每个阅读器识别它范围内的类别)
     * @param reader_m 阅读器
     */
    public void identify(Reader_M reader_m){
        Recorder recorder1 = reader_m.recorder;

        // 期望的标签列表, 可能有缺失；每个阅读器只覆盖了一个小范围, 但期望的标签列表是整个仓库的
        List<Tag> expectedTagList = environment.getExpectedTagList();

        List<Tag> coveredActualTagList = reader_m.coverActualTagList;
        // 计算阅读器范围内的意外标签
        environment.getAllTagList().removeAll(environment.getExpectedTagList());
        List<Tag> coveredUnexpectedTagList = reader_m.getReaderMOwnTagList(environment.getAllTagList());
        environment.getAllTagList().addAll(environment.getExpectedTagList());

        boolean useCLS;
        int recognizedCidNum = 0;
        int recognizedCidNumCurrentRound = 0;
        int recognizedMissCidNumCurrentRound = 0;
        int recognizedActualCidNumCurrentRound = 0;
        int filterVectorLength = 0; // filter vector长度
        int slotSumNum = 0; // 时隙数目

        // filter generation phase之前, 时隙中期望标签为0,1,2,3,>3的个数
        int[] beforeFilter = new int[]{0, 0, 0, 0, 0};
        int[] afterFilter = new int[]{0, 0, 0, 0, 0};
        // 实际回复的, 只能识别空时隙, 单时隙, 冲突时隙, 因此分别记录0,1,>=2的
        int[] realReply = new int[]{0, 0, 0};

        int twoTurnToZero = 0;
        int threeTurnToZero = 0;
        int twoTurnToOne = 0;
        int twoTurnToTwo = 0;
        int threeTurnToOne = 0;
        int threeTurnToTwo = 0;
        int threeTurnToThree = 0;

        int resolvedFromTwoCollision = 0;
        int resolvedFromThreeCollision = 0;

        Set<String> expectedActualCidSet = new HashSet<>();
        coveredActualTagList.forEach(tag -> expectedActualCidSet.add(tag.getCategoryID()));
        int expectedActualCidNum = expectedActualCidSet.size();

        Set<String> expectedCidSet = new HashSet<>();
        expectedTagList.forEach(tag -> expectedCidSet.add(tag.getCategoryID()));
//        int expectedCidNum = expectedCidSet.size();
        int expectedCidNum = LoF.estimate(environment.getActualTagList());

        logger.info("该阅读器需要识别的类别数=[ "+expectedCidNum+" ] 该阅读器识别范围内真实存在的类别数=[ "+expectedActualCidNum+" ]");
        double missRate = (expectedCidNum - expectedActualCidNum) * 1.0 / expectedCidNum;

        // 意外标签也会加入验证阶段,干扰实验结果,但不再期望标签中,因此在期望中不会给他分配时隙
        coveredActualTagList.addAll(coveredUnexpectedTagList);

        Map<Integer, List<Tag>> collisionTagListMap = new HashMap<>(); // 冲突时隙的时隙-标签列表映射
        int collisionTagListIndex = 0;
        int repeat = 0;
        while (recognizedCidNum < expectedCidNum) { // 当识别数目小于期望标签数目时一直循环
            ++recorder1.roundCount;

            logger.info("################### 第 "+recorder1.roundCount+" 轮 #######################");

            // CLS需要一个随机数, SFMTI需要两个随机数
            int random1 = (int) (100 * Math.random());
            int random2 = (int) (100 * Math.random());

            int frameSize = 0;
            double rho = 0;

            /**
             * 1 优化时隙
             */
            logger.error("缺失率 : " + missRate);
            if (missRate > 0.679){ // 缺失率>0.679, 使用cls
                frameSize = CLS_OptimizeFrame(missRate,expectedCidNum,recognizedCidNum);
                useCLS = true;
            }else{ // 缺失率<=0.679, 使用SFMTI
                frameSize = SFMTI_OptimizeFrame(expectedCidNum,recognizedCidNum);
                useCLS = false;
            }

            /**
             * 2 阅读器为标签分配时隙, 生成filter vector, expMap
             */
            logger.info("----------------生成filter vector--------------");
            logger.info("时隙 = ["+frameSize + "] 随机数1 = ["+random1 + "] 随机数2 = ["+random2 + "]");
            List<Integer> filterVector;
            if (useCLS){
                filterVector = this.CLS_genFilterVector(frameSize, random1, expectedTagList, beforeFilter, afterFilter, recorder1);
                logger.info("使用CLS");
            }else {
                filterVector = this.SFMTI_genFilterVector(frameSize, random1, random2, expectedTagList, beforeFilter, afterFilter, recorder1);
                logger.info("使用SFMTI");
            }

            // X是解决了冲突的冲突时隙和单时隙
            int X = 0;
            for (int i = 0; i < frameSize; i++){
                if (filterVector.get(i) > 0)
                    ++X;
            }

            // expMap 顾名思义 期望标签的映射, 键为时隙, 值为标签列表
            Map<Integer, List<Tag>> expMap = new HashMap<>();
            for (Tag tag : expectedTagList) {
                if(tag.isActive()){
                    if (useCLS)
                        CLS_SelectSlot(tag, random1, frameSize, filterVector, expMap, null);
                    else
                        SFMTI_SelectSlot(tag, random1, random2, frameSize, filterVector, X, expMap, null);
                }
            }

            Map<Integer, Set<String>> expSlotToCidSet = new HashMap<>();
            for(Integer slot : expMap.keySet()) {
                List<Tag> expTagList = expMap.get(slot);
                for(Tag tag : expTagList) {
                    if(tag.isActive()) {
                        if(!expSlotToCidSet.keySet().contains(slot)) {
                            Set<String> cidSet = new HashSet<>();
                            cidSet.add(tag.getCategoryID());
                            expSlotToCidSet.put(slot,cidSet);
                        } else {
                            Set<String> cidSet = expSlotToCidSet.get(slot);
                            cidSet.add(tag.getCategoryID());
                            expSlotToCidSet.put(slot,cidSet);
                        }
                    }

                }
            }

            /**
             * 3 验证阶段, 真实存在的标签回应, 生成verMap, resultMap
             */
            logger.info("-----------------------验证-----------------------");
            logger.debug("X: "+X);

            //Get the Map of the Tag to the corresponding tagList slot, so that it can be sorted and
            //displayed in ascending order according to the size of the slot when verifying
            // 键为filter vector的索引, 值为阅读器范围内的标签id和选择的时隙的列表,
            Map<Integer, List<String>> verMap = new TreeMap<>();
            for (Tag tag : coveredActualTagList) {
                if(tag.isActive()){
                    if (useCLS)
                        CLS_SelectSlot(tag, random1, frameSize, filterVector, null, verMap);
                    else
                        SFMTI_SelectSlot(tag, random1, random2, frameSize, filterVector, X, null, verMap);
                }
            }

            logger.debug("ver map: tagListSort output (sort in ascending order according to the size of the first Slot)：");
            // 打印verMap
            Iterator vit = verMap.keySet().iterator();
            while (vit.hasNext()){
                Integer key = (Integer) vit.next();
                List<String> strList = verMap.get(key);
                for (int i = 0; i < strList.size(); i++)
                    logger.debug(strList.get(i));
            }

            filterVector.removeIf(integer -> integer == 0);

            // 键: 时隙, 值: 回应的标签列表, 只有存在的标签, 没有缺失的标签
            Map<Integer, List<Tag>> resultMap = new HashMap<>();
            Map<Integer, Set<String>> actualSlotToCidSet = new HashMap<>();
            for (Tag tag: coveredActualTagList) {
                if(tag.isActive()){
                    int afterSlot = tag.getSlotSelected();
                    if (resultMap.containsKey(afterSlot)){
                        resultMap.get(afterSlot).add(tag);
                        actualSlotToCidSet.get(afterSlot).add(tag.getCategoryID());
                    }else {
                        List<Tag> nTagList = new ArrayList<>();
                        nTagList.add(tag);
                        resultMap.put(afterSlot, nTagList);

                        Set<String> actualCidSetInSlot = new HashSet<>();
                        actualCidSetInSlot.add(tag.getCategoryID());
                        actualSlotToCidSet.put(afterSlot,actualCidSetInSlot);
                    }
                }
            }

            // 打印resultMap和actualSlotToCidSet
            logger.debug("打印resultMap和actualSlotToCidSet：");
            for(Integer slot : resultMap.keySet()) {
                List<Tag> value = resultMap.get(slot);
                Set<String> tagstr = actualSlotToCidSet.get(slot);
                logger.debug("时隙 = "+ slot + " 标签ID = "+tagstr.toString());
                logger.debug("时隙 = "+ slot + "标签 = "+value.toString());
            }
//

            /**
             * 4 识别结果
             */
            logger.info("----------------------识别结果-------------------------");
            int slotLength = filterVector.size();
            int roundSlotCount = 0;

            if(useCLS) {
                for (int i = 0; i < slotLength; i++) {
                    if (filterVector.get(i) == 0) continue;
                    if (filterVector.get(i) == 1) {
                        if (!actualSlotToCidSet.containsKey(i)) {
                            // 全部缺失
                            realReply[0]++;
                            // 预期有标签回应(filterVector.get(i) > 0), 实际没有标签回应(slotResponseList.size() == 0), 全部缺失!
                            Set<String> str = expSlotToCidSet.get(i);
                            if (str.size() != 1) {
                                System.out.println("错误!");
                            }
                            expectedTagList.forEach(tag -> {
                                if (str.contains(tag.getCategoryID())) {
                                    tag.setActive(false);
                                }
                            });
                            recognizedCidNum += str.size();

                            recognizedCidNumCurrentRound += str.size();
                            recognizedMissCidNumCurrentRound += str.size();
                            recorder1.missingCids.addAll(str);
                            logger.info("时隙=" + (i) + " 结果=空时隙, 缺失类别列表=" + str.toString());
                            recorder1.emptySlotCount++;
                        } else {
                            // 认为该类别存在(有可能为意外标签的干扰,即假阳性错误)
                            String cid1 = expMap.get(i).get(0).getCategoryID();
                            expectedTagList.forEach(tag -> {
                                if (tag.getCategoryID().equals(cid1)) {
                                    tag.setActive(false);
                                }
                            });
                            recognizedActualCidNumCurrentRound++;
                            recognizedCidNumCurrentRound++;
                            recognizedCidNum++;
                            logger.info("时隙=" + (i) + " 结果=单时隙 " + cid1);
                            recorder1.singletonSlotCount++;
                            recorder1.actualCids.add(cid1);

                        }
                    } else {
                        if (!actualSlotToCidSet.containsKey(i)) {
                            // 冲突时隙但无标签回应,全部缺失
                            realReply[0]++;
                            if (filterVector.get(i) == 2) { // Unreconcilable 2 Collisions
                                twoTurnToZero++;
                            } else if (filterVector.get(i) == 3) { // Unreconcilable 3 Collisions
                                threeTurnToZero++;
                            }
                            // 预期有标签回应(filterVector.get(i) > 0), 实际没有标签回应(slotResponseList.size() == 0), 全部缺失!
                            Set<String> str = expSlotToCidSet.get(i);
                            expectedTagList.forEach(tag -> {
                                if (str.contains(tag.getCategoryID())) {
                                    tag.setActive(false);
                                }
                            });
                            recognizedCidNum += str.size();

                            recognizedCidNumCurrentRound += str.size();
                            recognizedMissCidNumCurrentRound += str.size();
                            recorder1.missingCids.addAll(str);
                            logger.info("时隙=" + (i) + " 结果=空时隙, 缺失类别列表=" + str.toString());
                            recorder1.emptySlotCount++;
                        }
                    }
                }
            } else {
                for (int i = 0; i < slotLength; i++) {
                    if(filterVector.get(i) == 0) continue;
                    // 下面全是预期有标签回应的情况
                    if(!actualSlotToCidSet.containsKey(i)) {
                        // 缺失!
                        String cid1 = expMap.get(i).get(0).getCategoryID();
                        expectedTagList.forEach(tag -> {
                            if (tag.getCategoryID().equals(cid1)) {
                                tag.setActive(false);
                            }
                        });
                        recognizedMissCidNumCurrentRound++;
                        recognizedCidNumCurrentRound++;
                        recognizedCidNum++;
                        logger.info("时隙=" + (i) + " 结果=单时隙 " + cid1);
                        recorder1.emptySlotCount++;
                        recorder1.missingCids.add(cid1);

                    } else {
                        // 存在
                        // 认为该类别存在(有可能为意外标签的干扰,即假阳性错误)
                        String cid1 = expMap.get(i).get(0).getCategoryID();
                        expectedTagList.forEach(tag -> {
                            if (tag.getCategoryID().equals(cid1)) {
                                tag.setActive(false);
                            }
                        });
                        recognizedActualCidNumCurrentRound++;
                        recognizedCidNumCurrentRound++;
                        recognizedCidNum++;
                        logger.info("时隙=" + (i) + " 结果=单时隙 " + cid1);
                        recorder1.singletonSlotCount++;
                        recorder1.actualCids.add(cid1);

                    }
                }
            }



            recorder1.roundSlotCountList.add(roundSlotCount);

            logger.debug("缺失率: "+missRate);
            logger.debug("帧长: " + frameSize+"; filter vector长度: " + filterVector.size() + ";");

            realReply[0]=0;
            realReply[1]=0;
            realReply[2]=0;

            recorder1.recognizedMissingCidNumList.add(recognizedMissCidNumCurrentRound);
            recorder1.recognizedActualCidNumList.add(recognizedActualCidNumCurrentRound);
            recorder1.recognizedCidNumList.add(recognizedMissCidNumCurrentRound+recognizedActualCidNumCurrentRound);
            recorder1.recognizedActualCidNum = recorder1.actualCids.size();
            recorder1.recognizedMissingCidNum = recorder1.missingCids.size();
            recorder1.recognizedCidNum = recorder1.actualCids.size() + recorder1.missingCids.size();


            if(expectedCidNum > recognizedCidNum) {
                recorder1.missingRateList.add( missRate);
                missRate = (expectedCidNum - expectedActualCidNum - recorder1.missingCids.size())*1.0 / (expectedCidNum - recorder1.missingCids.size()-recorder1.actualCids.size());
                // 由于意外标签的影响,缺失率可能出现异常值
                if(missRate >= 1) missRate = 0.99;
                if(missRate <= 0) missRate = 0.01;
            }
            if (recognizedCidNumCurrentRound == 0) {
                repeat ++;
            }
            if (repeat >= 5) {
                break;
            }

            // 清零
            recognizedActualCidNumCurrentRound = 0;
            recognizedMissCidNumCurrentRound = 0;
            recognizedCidNumCurrentRound = 0;
        }


        logger.info("----------------------------------------");
        logger.error("阅读器的总执行时间: [" + recorder1.totalExecutionTime+ " ms]");

        recorder1.recognizedActualCidNum = recorder1.actualCids.size();
        recorder1.recognizedMissingCidNum = recorder1.missingCids.size();
        recorder1.recognizedCidNum = recorder1.actualCids.size() + recorder1.missingCids.size();
        // 准确率,1-假阳性概率
        recorder1.correctRate = 1-(expectedCidNum - expectedActualCidNum-recorder1.recognizedMissingCidNum)*1.0/(expectedCidNum);
        System.out.println(" " );
    }
 
     private int SFMTI_OptimizeFrame(int expectedCidNum, int recognizedCidNum) {
        int f = (int)Math.ceil(((double)(expectedCidNum - recognizedCidNum)) / 1.68);
        return Math.max(f, 15);
    }

    private int CLS_OptimizeFrame(double missRate, int expectedTagNum, int recognizedTagNum) {
        // TODO 如果超过rho-opt-TSA.txt的情况.(因为有意外标签,缺失率可能大于1,会出现ArrayIndexOutOfBoundsException的情况)
        try{
            double rho = RHOUtils.getBestRho(missRate,"rho-opt-TSA.txt");
            int f = (int)Math.ceil(((double)(expectedTagNum - recognizedTagNum)) / rho);
            return Math.max(f,15);
        }catch (ArrayIndexOutOfBoundsException e) {
            return 15;
        }
    }

    private void SFMTI_SelectSlot(Tag tag, int random1, int random2, int frameSize, List<Integer> filterVector, int X, Map<Integer, List<Tag>> expMap, Map<Integer, List<String>> verMap) {
        int index = tag.hash2(frameSize, random1);
        int x1 = 0;
        int x2 = 0;
        int x3 = 0;
        for (int i = 0; i < index; i++){
            if (filterVector.get(i) == 1){
                x1++;
            }else if(filterVector.get(i) == 2){
                x2++;
            }else if(filterVector.get(i) == 3){
                x3++;
            }
        }
        int sumX = x1 + x2 + x3;
        String tmpStr;
        switch (filterVector.get(index)){
            case 0:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 0 (则afterSlot直接赋值 -1)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 + " afterSlot = -1 category ID = " + tag.getCategoryID();
                tag.fillMap(expMap, -1, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(-1);
                break;
            case 1:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 1 (则afterSlot应等于x1 + x2 + x3)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 + " afterSlot = "+sumX+" category ID  = " + tag.getCategoryID();
                tag.fillMap(expMap, sumX, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(sumX);
                break;
            case 2:
                int indexApp2 = tag.hash2(2,  random2);
                if (indexApp2 == 0){
                    tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 2 secondSlot = 0"+ " (则afterSlot应等于x1 + x2 + x3)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 + " afterSlot = "+sumX+" category ID  = " + tag.getCategoryID();
                    tag.fillMap(expMap, sumX, tag);
                    tag.fillMap(verMap, index, tmpStr);
                    tag.setSlotSelected(sumX);
                }else {
                    int tmpSlot = X + x2 + x3 * 2;
                    tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 2 secondSlot = "+indexApp2+ " (则afterSlot应等于X + x2 + x3 * 2)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 + "X = "+X+" afterSlot = "+tmpSlot+" category ID  = " + tag.getCategoryID();
                    tag.fillMap(expMap, tmpSlot, tag);
                    tag.fillMap(verMap, index, tmpStr);
                    tag.setSlotSelected(tmpSlot);
                }
                break;
            case 3:
                int indexApp3 = tag.hash2(3,  random2);
                if (indexApp3 == 0){
                    tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 3 secondSlot = 0 (则afterSlot应等于x1 + x2 +x3)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 + " afterSlot = "+sumX+" category ID  = " + tag.getCategoryID();
                    tag.fillMap(expMap, sumX, tag);
                    tag.fillMap(verMap, index, tmpStr);
                    tag.setSlotSelected(sumX);
                }else {
                    int thrSlot = X + x2 + x3 * 2 + indexApp3 - 1;
                    tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 3 secondSlot = "+indexApp3+" (则afterSlot应等于X + x2 + x3 * 2 + secondSlot - 1)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 + "X = "+X+ " afterSlot = "+thrSlot+" category ID  = " + tag.getCategoryID();
                    tag.fillMap(expMap, thrSlot, tag);
                    tag.fillMap(verMap, index, tmpStr);
                    tag.setSlotSelected(thrSlot);
                }
                break;
            default:
                break;

        }

    }

    private void CLS_SelectSlot(Tag tag, int random1, int frameSize, List<Integer> filterVector, Map<Integer, List<Tag>> expMap, Map<Integer, List<String>> verMap) {
        int index = tag.hash2(frameSize,random1);
        int x1 = 0;
        int x2 = 0;
        int x3 = 0;
        int x4 = 0;
        // 其实是跳过了空时隙的
        for (int i = 0; i < index; i++){

            if (filterVector.get(i) == 1){
                x1++;
            }else if(filterVector.get(i) == 2){
                x2++;
            }else if(filterVector.get(i) == 3){
                x3++;
            }else if(filterVector.get(i) > 3){
                x4++;
            }
        }
        int sumX = x1 + x2 + x3 + x4;
        String tmpStr = "";
        // generate filter vector phase, 从原始分配的时隙-标签映射到跳过了空时隙的时隙-标签映射列表
        switch (filterVector.get(index)){
            case 0:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 0 (则afterSlot直接赋值 -1) category ID = " + tag.getCategoryID();
                tag.fillMap(expMap, -1, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(-1);
                break;
            case 1:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 1 (则afterSlot应等于x1 + x2 + x3 + x4)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 +"  X4 = "+x4 + " afterSlot = "+sumX+" category ID = " + tag.getCategoryID();
                tag.fillMap(expMap, sumX, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(sumX);
                break;
            case 2:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 2 (则afterSlot应等于x1 + x2 + x3 + x4)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 +"  X4 = "+x4 + " afterSlot = "+sumX+" category ID = " + tag.getCategoryID();
                tag.fillMap(expMap, sumX, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(sumX);
                break;
            case 3:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 = 3 (则afterSlot应等于x1 + x2 +x3 + x4)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 +"  X4 = "+x4 + " afterSlot = "+sumX+" category ID = " + tag.getCategoryID();
                tag.fillMap(expMap, sumX, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(sumX);
                break;
            default:
                tmpStr = "[inner]slot = "+index+" 对应vector中的值 > 3 (则afterSlot应等于x1 + x2 +x3 + x4)"+" x1 = " + x1 + " X2 = "+x2 +"  X3 = "+x3 +"  X4 = "+x4 + " afterSlot = "+sumX+" category ID = " + tag.getCategoryID();
                tag.fillMap(expMap, sumX, tag);
                tag.fillMap(verMap, index, tmpStr);
                tag.setSlotSelected(sumX);
                break;
        }

    }

    // 实现时, CLS 使用前后, filter vector没变, 论文中提到0->00, 1->01, >=2 -> 1, 实现时只在计算时间部分考虑了
    public List<Integer> CLS_genFilterVector(int frameSize, int random1, List<Tag> theTagList,int[] beforeFilter, int[] afterFilter, Recorder recorder1){
        int[] before = new int[]{0,0,0,0,0};
        int[] after = new int[]{0,0,0,0,0};
        // 本轮标签将会有回应的时隙数量
        int responseNum = 0;

        VectorMap vectorMap = VectorMap.genBaseVectorMap2(frameSize, random1, 0, theTagList);
        List<Integer> filterVector = vectorMap.getFilterVector();
        Map<Integer, List<Tag>> resultMap = vectorMap.getResultMap();
        // 统计在使用CLS前, filter vector中各类时隙有多少, 比如, 空时隙有多少, 单时隙有多少, 等等
        for (int i = 0; i <filterVector.size(); i++){
            int tmpValue = filterVector.get(i);
            if (tmpValue == 0) {
                beforeFilter[0]++;
                before[0] ++;
            }
            else if (tmpValue == 1) {
                beforeFilter[1]++;
                before[1]++;
            }
            else if (tmpValue == 2) {
                beforeFilter[2]++;
                before[2]++;
            }
            else if (tmpValue == 3) {
                beforeFilter[3]++;
                before[3]++;
            }
            else if (tmpValue > 3) {
                beforeFilter[4]++;
                before[4]++;
            }
        }
        logger.info("在使用CLS之前, filter vector的时隙类型 0,1,2,3,>3: 0:["+before[0]+"个] 1:["+before[1]+"个]"+" 2:["+before[2]+"个] 3:["+before[3]+"个] >3:["+before[4]+"个]");

        int len = filterVector.size();
        logger.info("生成filter vector: ");
        // 打印filter vector的内容
        for (int k = 0; k < len; k++) {
            int element = filterVector.get(k);
            if (element >= 2) {
                List<String> tagStr = new ArrayList<>();
                for (int i = 0; i < resultMap.get(k).size(); i++)
                    tagStr.add(resultMap.get(k).get(i).getCategoryID());
                logger.debug("时隙 = " + k + " 类别 = " + tagStr.toString());
            } else if (element == 1) {

//                logger.debug("时隙 = " + k + " 类别 = " + resultMap.get(k).get(0).getCategoryID());
            } else if (element == 0) {
                logger.debug("时隙 = " + k+" 空时隙");
            }
        }


        logger.info("最终的 filterVector: ["+filterVector.toString() + "]");
        // CLS 直接跳过空时隙, 不要计算空时隙的时间, 即计算时间时不考虑afterFilter[0]
        for (int i = 0; i <filterVector.size(); i++){
            int tmpValue = filterVector.get(i);
            if (tmpValue == 0) {
                afterFilter[0]++;
                after[0]++;
            } else if (tmpValue == 1) {
                afterFilter[1]++;
                after[1]++;
                responseNum++;
            } else if (tmpValue == 2) {
                afterFilter[2]++;
                after[2]++;
                responseNum++;
            } else if (tmpValue == 3) {
                afterFilter[3]++;
                after[3]++;
                responseNum++;
            } else if (tmpValue > 3) {
                afterFilter[4]++;
                after[4]++;
                responseNum++;
            }
        }

        double t1 = clsRoundExecutionTime(filterVector, responseNum);
        recorder1.totalExecutionTime += t1;
        recorder1.executionTimeList.add(t1);
        logger.info("本轮时间: " + t1);
        logger.info("使用CLS之后, filter vector中的时隙类型 0,1,2,3,>3: 0:["+after[0]+"个] 1:["+after[1]+"个]"+" 2:["+after[2]+"个] 3:["+after[3]+"个] >3:["+after[4]+"个]");
        return filterVector;
    }

    public List<Integer> SFMTI_genFilterVector(int frameSize, int random1, int random2, List<Tag> theTagList, int[] beforeFilter, int[] afterFilter, Recorder recorder1){

        // beforeFilter[]和afterFilter[]是累加的, 记录的是所有轮次的数据
        // before[],after[]是在本轮, 使用SFMTI前后, 各种类型的时隙个数
        int[] before = new int[]{0,0,0,0,0};
        int[] after = new int[]{0,0,0,0,0};

        VectorMap vectorMap = VectorMap.genBaseVectorMap2(frameSize, random1, 0, theTagList);
        List<Integer> filterVector = vectorMap.getFilterVector();
        if (filterVector == null)
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++ERROR FILTER VECTOR NULL+++++++++++++++++++++++++++++++++++++");
        Map<Integer, List<Tag>> resultMap = vectorMap.getResultMap();
        Map<Integer, Set<String>> slotToCidSet = vectorMap.getSlotToCidSet();

        for (int i = 0; i <filterVector.size(); i++){
            int tmpValue = filterVector.get(i);
            if (tmpValue == 0) {
                before[0]++;
                beforeFilter[0]++;
            }
            else if (tmpValue == 1) {
                before[1]++;
                beforeFilter[1]++;
            }
            else if (tmpValue == 2) {
                before[2] ++;
                beforeFilter[2]++;
            }
            else if (tmpValue == 3) {
                before[3]++;
                beforeFilter[3]++;
            }
            else if (tmpValue > 3) {
                before[4]++;
                beforeFilter[4]++;
            }
        }
        logger.info("在使用SFMTI之前, filter vector的时隙类型 0,1,2,3,>3: 0:["+before[0]+"个] 1:["+before[1]+"个]"+" 2:["+before[2]+"个] 3:["+before[3]+"个] >3:["+before[4]+"个]");
        int beforeSize = filterVector.size();
        logger.info("生成 Filter Vector: ");
        int oriFilterVectorSize = filterVector.size();
        for (int k = 0; k < oriFilterVectorSize; k++){
            int element = filterVector.get(k);
            if(element > 3){
                filterVector.set(k, 0); // 大于3的冲突时隙设为0, 忽略
                logger.debug("时隙 = " + k + " 类别 = "+ resultMap.get(k));
            }else if(element == 2 || element == 3){
                int total = element;
                Map<Integer, Set<String>> secondSlotToCidSet = new HashMap<>();

                for (Tag tag : theTagList){
                    if(tag.getSlotSelected() == k) {
                        int secondSlot = tag.hash2(total,  random2); // 调和冲突

                        logger.debug("第一次选择的时隙 = " + k + " 第二次选择的时隙 = "+secondSlot + " 类别 = "+tag.getCategoryID());
                        if(!secondSlotToCidSet.containsKey(secondSlot)) {
                            Set<String> cidSetInSecondSlot = new HashSet<>();
                            cidSetInSecondSlot.add(tag.getCategoryID());
                            secondSlotToCidSet.put(secondSlot,cidSetInSecondSlot);
                        } else {
                            secondSlotToCidSet.get(secondSlot).add(tag.getCategoryID());
                        }

                    }

                }
                Iterator iterator = secondSlotToCidSet.keySet().iterator();
                // while这段是打印map, map的键为: 时隙, 值为该时隙对应的标签个数
                while (iterator.hasNext()){
                    Integer key = (Integer) iterator.next();
                    Integer value = secondSlotToCidSet.get(key).size();
                    logger.debug("[inner] secondSlotToCidSet key = "+key+" value = "+value);
                }

                boolean flag = true;
                if(secondSlotToCidSet.size() < element) {
                    flag = false;

                }
                else {
                    for(Integer key :secondSlotToCidSet.keySet()) {
                        if (secondSlotToCidSet.get(key).size() != 1) {
                            flag = false;

                            break;
                        }
                    }
                }
                if(flag) {
                    for (int m = 0; m < total - 1; m++)
                        filterVector.add(1);
                } else {
                    filterVector.set(k, 0);
                }


            }else if (element == 1) {
//                logger.debug("时隙 = " + k + " 类别 = "+resultMap.get(k).get(0).getCategoryID()); // TODO?bug!
            }else if(element == 0){
                logger.debug("时隙 = " + k);
            }
            logger.debug("[inner] tmpFilter:"+ filterVector);
        }

        logger.info("最终的filter vector: ["+ filterVector + "]");
        for (int i = 0; i <filterVector.size(); i++){
            int tmpValue = filterVector.get(i);
            if (tmpValue == 0){
                afterFilter[0]++;
                after[0]++;
            }

            else if (tmpValue == 1){
                afterFilter[1]++;
                after[1]++;
            }

            else if (tmpValue == 2){
                afterFilter[2]++;
                after[2]++;
            }

            else if (tmpValue == 3){
                afterFilter[3]++;
                after[3]++;
            }

            else if (tmpValue > 3){
                afterFilter[4]++;
                after[4]++;
            }

        }


        double t1= sfmtiRoundExecutionTime(beforeSize, after[1]+after[2]+after[3]);
        recorder1.totalExecutionTime += t1;
        recorder1.executionTimeList.add(t1);
        logger.info("本轮识别时间: " + t1);
        logger.debug("在使用SFMTI之后. filter vector:"+filterVector);
        logger.info("在使用SFMTI之后, filter vector的时隙类型 0,1,2,3,>3: 0:["+after[0]+"个] 1:["+after[1]+"个]"+" 2:["+after[2]+"个] 3:["+after[3]+"个] >3:["+after[4]+"个]");
        return filterVector;
    }




    /** 一轮SFMTI的执行时间
     *
     * @param beforeSize
     * @param roundSlots
     */
    public double sfmtiRoundExecutionTime(int beforeSize, int roundSlots){
        double t1 = (double) Math.ceil(beforeSize * 2.0 /96) * 2.4 + roundSlots * 0.4;
        return t1;
    }

    /**
     * 一轮CLS的执行时间
     * @param filterVector
     * @param responseSlotsNum 发送短消息的时隙数量
     */
    public double clsRoundExecutionTime(List<Integer> filterVector, int responseSlotsNum){
        int twoBit = 0;
        int oneBit = 0;

        for (int i = 0; i < filterVector.size(); i++){
            if (filterVector.get(i) == 0 || filterVector.get(i) == 1){
                twoBit++;
            } else {
                oneBit++;
            }
        }
        double readerReqTime = twoBit * 2 * 2.4 / 96 + oneBit * 2.4 / 96;

        double tagReplyTime = responseSlotsNum * 0.4;
        double t1 = readerReqTime + tagReplyTime;
        return t1;
    }
}


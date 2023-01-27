package protocals;

import base.Tag;
import utils.Environment;
import utils.Recorder;

import java.util.*;
import org.apache.logging.log4j.Logger;

public class SEM {
    private Map<String,String> cateMap = new HashMap<>();
    private List<String> cateList = new ArrayList<>();
    private Logger logger;
    private Recorder recorder;
    private Environment environment;
    private List<String> physicalFrame = new ArrayList<>();
    private List<String> logicalFrame = new ArrayList<>();

    private int k;
    private int f;
    private int f2;
    private int nx=100;//具有最少标签数的类别的标签数
    private int ny=100;//具有最多标签数的类别的标签数

    private int cateNum = 0;

    private double alpha = 0.05;//confidence
    private double beta = 0.95;//accuracy

    public SEM(Logger logger, Recorder recorder, Environment environment) {
        this.logger = logger;
        this.recorder = recorder;
        this.environment = environment;
        this.nx = environment.getNx();
        this.ny = environment.getNy();
    }

    public void initSOstr(List<Tag> tagList) {
        Set<String> cidSet = new HashSet<>();
        for(Tag tag : tagList) {
            cidSet.add(tag.categoryID);
        }
        int i = 0;
        for(String cid : cidSet) {
            String SOstr = genSOstr(cidSet.size(),i);
            cateMap.put(cid,SOstr);
            cateList.add(cid);
            i++;
        }
        for(Tag tag : tagList) {
            tag.setSOstr(cateMap.get(tag.categoryID));
        }
        cateNum = cidSet.size();

        logger.info("##############初始化SO字符串##############");
        logger.info("期望的类别数为:"+cateNum);
        logger.info("初始化的SO字符串为:");
        for(String cid : cateMap.keySet()) {
            logger.info("CID:"+cid+" SO字符串:"+cateMap.get(cid));
        }
    }

    public void response(int f,int rand) {
        // 初始化physical frame, 每个时隙收到的曼彻斯特编码为000...000
        StringBuilder initRespSb = new StringBuilder();
        for(int i = 0; i < cateNum; i++) {
            initRespSb.append("0");
        }
        String initRespStr = initRespSb.toString();
        for(int i = 0; i < f2; i++) {
            physicalFrame.add(initRespStr);
        }
        List<Tag> actualTagList = environment.getActualTagList();
        for(Tag tag : actualTagList) {
            int slot = tag.hash1(f,rand);
            if(slot>=f2) continue;
            String newData = encode(physicalFrame.get(slot),tag.getSOstr());
            physicalFrame.set(slot,newData);
        }

        logger.info("##############标签回应阅读器SO字符串##############");
        logger.info("参数f,f2="+f+","+f2);
        logger.info("生成的物理帧如下:");
        for(int i = 0; i < f2;i++) {
            logger.info("slot:"+i+" data:"+physicalFrame.get(i));
        }

    }

    public void decode() {
        int frameSize = f2;
        for(int i = 0; i < cateNum; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < frameSize; j++) {
                if (physicalFrame.get(j).charAt(i) == '0') {
                    sb.append("0");
                } else {
                    sb.append("1");
                }
            }
            logicalFrame.add(sb.toString());
        }

        logger.info("##############阅读器解析物理帧,生成逻辑帧##############");
        logger.info("逻辑帧如下:");
        for(int i = 0; i < cateNum;i ++) {
            logger.info("第"+i+"个类,CID="+cateList.get(i)+",逻辑帧为:"+logicalFrame.get(i));
        }
    }

    public void identify() {
        initSOstr(environment.getExpectedTagList());
        optimizeParams();
        for(int i1 = 0; i1 < k; i1++) {
            logger.info("++++++++++++第"+i1+"次++++++++++++");
            long t = System.currentTimeMillis();
            Random rd = new Random(t);
            int rand = rd.nextInt();
            logger.info("rand="+rand);
            response(f, rand);
            decode();
            for (int i = 0; i < cateNum; i++) {
                String bitVec = logicalFrame.get(i);
                String cid = cateList.get(i);
                // 估算该类别的标签数
                String subBitVec = bitVec.substring(0, f2 - 1);
                if (subBitVec.indexOf('1') != -1 ) {
                    recorder.actualCids.add(cid);
                }
            }
            for(int i = 0; i < cateNum; i++) {
                String cid = cateList.get(i);
                if(!recorder.actualCids.contains(cid)){
                    recorder.missingCids.add(cid);
                }
            }
        }
        System.out.println("缺失的类别数:"+recorder.missingCids.size());
        System.out.println("存在的类别数:"+recorder.actualCids.size());

        logger.info("##############识别存在和缺失的类别##############");
        logger.info("缺失的类别数:"+recorder.missingCids.size());
        logger.info("缺失的类别:");
        for(String cid : recorder.missingCids) {
            logger.info(cid);
        }
        logger.info("存在的类别数:"+recorder.actualCids.size());
        for(String cid : recorder.actualCids) {
            logger.info(cid);
        }
    }

    private String encode(String s1, String s2){
        int i = 0;
        StringBuilder stringBuilder = new StringBuilder();
        while(i < s1.length()){
            if(s1.charAt(i) == s2.charAt(i)){
                stringBuilder.append(s1.charAt(i));
            }else{
                // If the bits at the same position in strings from different tags are not the same, the reader will decode this bit as ‘X’
                stringBuilder.append('X');
            }

            i++;
        }
        return stringBuilder.toString();
    }

    private String genSOstr(int leng, int i) {
        StringBuilder str = new StringBuilder();
        for(int k = 0; k < leng; k++) {
            if(k == i) {
                str.append("1");
            } else {
                str.append("0");
            }
        }
        return str.toString();
    }

    private void optimize_k(int ni) {
        double k1 = Math.pow(f*1.96/(alpha*ni),2)*(Math.exp(ni*1.0/f)-1)/f2;
        k = (int)k1;
    }

    private double optimize_f2_and_f(int ni) {
        int f2_temp;
        double minTime = Double.POSITIVE_INFINITY;
        int tempf;
        int tempf2;
        double tempMinTime;
        for(f2_temp = 1;f2_temp<=512;f2_temp++){
            logger.info("+++++++++++++f2_temp="+f2_temp+"+++++++++++++");
            int left = f2_temp;
            int right = 3*ny;
            if(left>right) {
                int tmp = left;
                left = right;
                right = tmp;
            }
            int mid = left + (right-left)/2;
            while(left < right-1) {
                mid = left + (right-left)/2;
                logger.info("mid,left,right,first_order<0?"+mid+" "+left+" "+right+" "+(getTimeFirstOrder(mid,f2_temp,ni)<0));
                if(getTimeFirstOrder(mid,f2_temp,ni)<0) {
                    left = mid;
                } else {
                    right = mid;
                }
            }
            tempf = mid;
            tempf2 = f2_temp;
            tempMinTime = getTime(tempf,tempf2,ni);
            logger.info("tempMinTime,minTime="+tempMinTime+" "+minTime);
            if(tempMinTime < minTime) {
                minTime = tempMinTime;
                f = tempf;
                f2 = tempf2;
            }

        }
        return minTime;
    }

    // 测试,变成public
    public double optimizeParams() {
        double minTime;
        if(nx==ny) {
            double minTime1 = optimize_f2_and_f(nx);
            minTime = minTime1;
            optimize_k(nx);
        } else {
            double minTime1 = optimize_f2_and_f(nx);
            int tempfx = f;
            int tempf2x = f2;
            double minTime2 = optimize_f2_and_f(ny);
            if(minTime1>minTime2) {
                minTime = minTime1;
                f = tempfx;
                f2 = tempf2x;
                optimize_k(nx);
            } else {
                minTime = minTime2;
                optimize_k(ny);
            }

        }
        recorder.totalExecutionTime = minTime;
        System.out.println("优化系数:k,f,f2,minTime="+k+" "+f+" "+f2+" "+minTime);
        logger.info("##############优化系数##############");
        logger.info("系数:k,f,f2,minTime="+k+" "+f+" "+f2+" "+minTime);
        return minTime;
    }

    // 测试，改成public
    public double getTime(int f,int f2,int ni) {
//        double tb = cateNum*0.025; //TODO 常数,先按论文里给的常数复现论文的结果
//        double t = Math.pow(f*1.96,2)*(Math.exp(ni*1.0/f)-1)*(tb+f2*0.4)/(f2*Math.pow(alpha*ni,2));
        // TODO 按论文里给的常数复现论文的结果
        double tb = cateNum*0.0188; //TODO 常数,先按论文里给的常数复现论文的结果
        double t = Math.pow(f*1.96,2.0)*(Math.exp(ni*1.0/f)-1)*(tb+f2*0.302)/(f2*Math.pow(alpha*ni,2.0));
        return t;
    }

    private double getTimeFirstOrder(int f,int f2,int ni) {
//        double tb = cateNum*0.025; // TODO 常数,
//        double t = (tb+f2*0.4)*1.96*1.96/(f2*(Math.pow(alpha*ni,2)))*(2*f*(Math.exp(ni*1.0/f)-1)-Math.exp(ni*1.0/f)*ni);
        double tb = cateNum*0.0188; //TODO 常数,先按论文里给的常数复现论文的结果
        double t = (tb+f2*0.302)*1.96*1.96/(f2*(Math.pow(alpha*ni,2.0)))*(2*f*(Math.exp(ni*1.0/f)-1)-Math.exp(ni*1.0/f)*ni);
        return t;
    }


}

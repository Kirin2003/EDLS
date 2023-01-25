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
    private int nx;//具有最少标签数的类别的标签数
    private int ny;//具有最多标签数的类别的标签数

    private double alpha = 0.05;//confidence
    private double beta = 0.95;//accuracy

    public SEM(Logger logger, Recorder recorder, Environment environment) {
        this.logger = logger;
        this.recorder = recorder;
        this.environment = environment;
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
    }

    public void response(int f,int rand) {
        // 初始化physical frame, 每个时隙收到的曼彻斯特编码为000...000
        int cateNum = cateMap.size();
        StringBuilder initRespSb = new StringBuilder();
        for(int i = 0; i < cateNum; i++) {
            initRespSb.append("0");
        }
        String initRespStr = initRespSb.toString();
        for(int i = 0; i < f; i++) {
            physicalFrame.add(initRespStr);
        }
        List<Tag> actualTagList = environment.getActualTagList();
        for(Tag tag : actualTagList) {
            int slot = tag.hash1(f,rand);
            String newData = encode(physicalFrame.get(slot),tag.getSOstr());
            physicalFrame.set(slot,newData);
        }
    }

    public void decode() {
        int cateNum = cateMap.size();
        int frameSize = physicalFrame.size();
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
    }

    public void identify() {
        initSOstr(environment.getExpectedTagList());
        int f = 100;//时隙长怎么取 TODO
        int rand = (int) (100 * Math.random());
        response(f,rand);
        decode();
        int cateNum = cateMap.size();
        for(int i = 0; i < cateNum; i++) {
            String bitVec = logicalFrame.get(i);
            String cid = cateList.get(i);
            if(bitVec.contains("1") || bitVec.contains("X")) {
                recorder.actualCids.add(cid);
            } else {
                recorder.missingCids.add(cid);
            }
        }
        System.out.println("缺失的类别数:"+recorder.missingCids.size());
        System.out.println("存在的类别数:"+recorder.actualCids.size());
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

    private void optimize_k() {
        double k1 = Math.pow(f*1.96/(alpha*nx),2)*(Math.exp(nx/f)-1)/f2;//TODO,是(alpha*nx)吗
        k = (int)k1;
    }

    private double optimize_f2_and_f() {
        int f2;
        double minTime = Double.POSITIVE_INFINITY;
        for(f2 = 1;f2<=512;f2++){
            int left = f2;
            int right = 3*ny;
            int mid;
            while(left < right) {
                mid = left + (right-left)/2;
                if(getTimeFirstOrder(mid,f2,ny)<0) {//TODO,是ny吗?
                    left = mid;
                } else {
                    right = mid;
                }
            }
            if(nx==ny) {
                double time1 = getTime(left,f2,ny);
                if(time1 < minTime) {
                    minTime = time1;
                }
            } else {
                double time1 = getTime(left,f2,nx);
                double time2 = getTime(left,f2,ny);
                if(Math.max(time1,time2)<minTime) {
                    minTime = Math.max(time1,time2);
                }
            }


        }
        return minTime;
    }

    private double getTime(int f,int f2,int ni) {
        double t1 = 0;//TODO
        double t = Math.pow(f*1.96,2)*(Math.exp(ni*1.0/f)-1)*(t1+f2*0.4)/(f2*Math.pow(alpha*ni,2));
        return t;
    }

    private double getTimeFirstOrder(int f,int f2,int ni) {
        double t1 = 0;//TODO
        double t = (t1+f2*0.4)*(Math.pow(f*1.96,2)/(f2*alpha*alpha*ni*ni)*(-2/ni*(Math.exp(ni/f)-1)+Math.exp(ni/f)/f));
        return t;
    }

}

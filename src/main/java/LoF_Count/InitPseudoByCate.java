package LoF_Count;

import base.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kirin Huang
 * @date 2022/11/14 下午9:43
 */
public class InitPseudoByCate {
    /**
     * Add: 初始化标签的PseudoRandomList, 同一类别的PseudoRandomList一致
     */
    public static void initPseudoRandomListByCate(List<Tag> tagList, int pseudoRanStrLen, int pseudoRanListLen) {
        Map<String,List<Tag>> CateMap = new HashMap<>();
        for(Tag tag : tagList) {
            String cid = tag.getCategoryID();
            if(!CateMap.containsKey(cid)){
                List<Tag> tagList1 = new ArrayList<>();
                tagList1.add(tag);
                CateMap.put(cid,tagList1);
            } else{
                CateMap.get(cid).add(tag);
            }
        }
        for(String cid : CateMap.keySet()){
            List<String> pseudoRandomList1 = new ArrayList<>();
            for (int i = 0; i < pseudoRanListLen; i++){
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < pseudoRanStrLen; j++){
                    sb.append(((int)(10 * Math.random()))%2);
                }
                pseudoRandomList1.add(sb.toString());

            }
            for(Tag tag : CateMap.get(cid)) {
                tag.setPseudoRandomList(pseudoRandomList1);
                tag.setPseudoRanListLen(pseudoRanListLen);
                tag.setPseudoRanStrLen(pseudoRanStrLen);
            }
        }
    }
}

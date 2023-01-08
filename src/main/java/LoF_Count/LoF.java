package LoF_Count;

import base.Tag;

import java.util.*;

/**
 * 本类为LoF的实现类，用来估算区域内的标签个数
 * 
 * @author LiMingzhe
 *
 */
public class LoF {
	public static int hashStrLength = 100;
	public static int hashNum = 15;

	/**
	 * 计算哈希值，哈希值为tag里0、1序列的从右往左的第一个1的位置
	 * 
	 * @param tag 标签
	 * @return 哈希值
	 */
	public static int hash(Tag tag) {
		String hashNum = tag.getPseudoRandomList().get(10);
		return hashNum.length() - 1 - hashNum.lastIndexOf("1");
	}

	public static int hash2(String str){
		return str.length() - 1 - str.lastIndexOf("1");
	}

	public static Map<String,List<String>> genMap(List<Tag> tagList) {
		Set<String> cidset = new HashSet<>();
		for(Tag tag : tagList){
			cidset.add(tag.getCategoryID());
		}
		System.out.println("real cid num:"+cidset.size());
		Map<String,List<String>> cidRandomlistMap = new HashMap<>();
		for(String cid : cidset){
			List<String> randomList = new ArrayList<>();
			for (int i = 0; i < hashNum; i++){
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < hashStrLength; j++)
					sb.append(((int)(10 * Math.random()))%2);
				randomList.add(sb.toString());

			}
			cidRandomlistMap.put(cid,randomList);
		}
		for(Tag tag : tagList) {
			tag.setPseudoRandomList(cidRandomlistMap.get(tag.categoryID));
			tag.setPseudoRanStrLen(hashStrLength);
			tag.setPseudoRanListLen(hashNum);
		}
		return cidRandomlistMap;
	}

	/**
	 * 估计标签数量
	 * 
	 * @return 估计的数量
	 */
	public static int estimate(List<Tag> tagList) {
		// add
		genMap(tagList);
		// 获得标签hash长度
		int hashLength = tagList.get(0).getPseudoRandomList().get(10).length();

		// 记录slot信息的数组
		byte[] slotInfo = new byte[hashLength];

		//system.out.println("info of estimation:--------------------");

		// 读取读写器覆盖的标签里的哈希值，用来确定在哪个slot返回
		for (int i = 0; i < hashLength; i++) {
			for (Tag t : tagList) {
				if (hash(t) == i) {
						slotInfo[hashLength - 1 - i] = 1;
					// 打印在该slot返回的tagID
					//system.out.println("tagID:" + t.getTagID() + " " + "tagHash:" + t.getPseudoRandomList().get(10) + " "+ "selectSlot:" + hash(t));
				}
			}
		}

		//system.out.println("slotInfo:");
		for (int i = 0; i < slotInfo.length; i++) {
			//system.out.print(slotInfo[i] + " ");
		}

		// R是数组allSlotInfo最右边0的位置
		int R = 0;
		for (int i = slotInfo.length - 1; i > 0; i--) {
			if (slotInfo[i] == 0) {
				R = hashLength - 1 - i;
				break;
			}
		}
		//system.out.println();
		//system.out.println("--------");
		// n为估计的标签数量
		int n = (int) (1.2897 * Math.pow(2, R));
		System.out.println("the estimate tagNum:" + n);
		System.out.println("--------");
		
		return n;
	}

	/**
	 * 估计标签列表中包含的类别数量
	 *
	 * @return 估计的数量
	 */
	public static int estimate2(List<Tag> tagList) {
		Map<String,List<String>> cidRandomListMap = genMap(tagList);
		// 获得标签hash长度
		int hashLength = hashStrLength;

		// 记录slot信息的数组
		byte[] slotInfo = new byte[hashLength];

		// 生成bit map
		for(String cid : cidRandomListMap.keySet()) {
			String random = cidRandomListMap.get(cid).get(10);
			int i1 = hash2(random);
			slotInfo[hashLength-1-i1]=1;
		}

		// 估算
		int R = 0;//R是slotInfo中最右边0的位置
		for(int i = slotInfo.length-1; i>0;i--){
			if(slotInfo[i]==0){
				R=hashLength-1-i;
				break;
			}
		}

		// n为估计的标签数量
		int n = (int) (1.2897 * Math.pow(2, R));
		System.out.println("the estimate tagNum:" + n);
		System.out.println("--------");

		return n;


	}

}

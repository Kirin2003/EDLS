package LoF_Count;

import base.Tag;

import java.util.*;
import java.util.Map.Entry;

/**
 * 本类在基础LoF基础上增加了Multi splitting机制来使标签的估计值更精确
 * @author LiMingzhe
 *
 */
public class MultisplittingLoF {
	public static int hashStrLength = 100;
	public static int hashNum = 15;
	
	/**
	 * 键为标签集合，值为第几轮计算标签总量
	 * 在每一次标签总量的计算过程中将每一个冲突slot的标签集放入该map
	 */
	private static Map<List<Tag>, Integer> tagListMap = new HashMap<>();
	
	/**
	 * 在每一次标签总量的计算过程中将每一个有且只有一个标签的slot的标签集放入该map
	 */
	private static Map<Tag, Integer> identifiedTag = new HashMap<>();
	
	/**
	 * 计算哈希值，哈希值为tag里0、1序列的从右往左的第一个1的位置
	 * @param tag 标签
	 * @param num 取标签中哪一个伪随机数
	 * @return 哈希值
	 */
	public static int hash(Tag tag, int num) {
		String hashNum = tag.getPseudoRandomList().get(num);
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
		return cidRandomlistMap;
	}
	
	/**
	 * 估计标签数量
	 * @return 估计的数量
	 */
	public static int estimate(List<Tag> tagList, double errorRate) {
		
		// 获得循环的数量
		int hashNum = tagList.get(0).getPseudoRanListLen();
				
		// 将所有标签放入tagListMap，以进行第一次估计
		tagListMap.put(tagList, 1);
				
		// 循环次数
		int round = 1;		
		
		List<Integer> sum = new ArrayList<>();
		
		// 进行第一次循环，并将估算结果放入sum集合
		int n1 = 0;
		ArrayList<List<Tag>> tagListArr1 = getTagList(tagListMap, round);
		for (List<Tag> t : tagListArr1) {
			n1 += estimate(t, round);
		}
		round++;
		
		//System.out.println("The round 1 estimation is " + n1);
		//System.out.println("The identified Tag's size is " + (identifiedTag.size() - getTag(identifiedTag, round).size()));
		//System.out.println("The totle estimation number is " + n1);
		//System.out.println();
		sum.add(n1);
		
		while (round <= hashNum) {
			int n = 0;		// n为估计的总数，n的数量为这一轮所有tagList的估计总和加上所有的identifiedTag
			ArrayList<List<Tag>> tagListArr = getTagList(tagListMap, round);
			if (tagListArr.size() == 0) {	//如果为空，说明所有标签均已被识别
				//System.out.println("All tags are identified!");
				//System.out.println("The accurate number of tags is " + identifiedTag.size());
				sum.add(identifiedTag.size());
				break;
			}
			for (List<Tag> t : tagListArr) {
				n += estimate(t, round);
			}
			
			//System.out.println("The round " + round +" estimation is " + n);
			//System.out.println("The identified Tag's size is " + (identifiedTag.size() - getTag(identifiedTag, round).size()));
			n += identifiedTag.size() - getTag(identifiedTag, round).size();
			//System.out.println("The totle estimation number is " + n);
			//System.out.println();
			sum.add(n);
			
			double sum1 = sum.get(sum.size() - 1);
			double sum2 = sum.get(sum.size() - 2);
			double errorRateInTheRound = Math.abs(sum1 - sum2) / sum2;
			if (errorRateInTheRound <= errorRate) {
				break;
			}
			round++;
		}
		
//		System.out.println("the estimate num in every round:----------");
//		for (int n : sum) {
//			System.out.print(n + " ");
//		}
//		System.out.println("----------");
		return sum.get(sum.size() - 1);
	}
	
	/**
	 * 估算给定标签集合的标签数
	 * @param tagList 需要进行估算标签集
	 * @param round 第几次迭代
	 * @return 估计的总数
	 */
	private static int estimate(List<Tag> tagList, int round) {
		
		// 获得标签hash长度
		int hashLength = tagList.get(0).getPseudoRandomList().get(10).length();


		// 记录slot信息的数组
		byte[] slotInfo = new byte[hashLength];
		
		//System.out.println("the " + round + " round starts:----------");
		
		// 记录每一个标签的slot
		Map<Tag, Integer> slotResponse = new HashMap<>();
		
		// 读取读写器覆盖的标签里的哈希值，用来确定在哪个slot返回
		for (int i = 0; i < hashLength; i++) {
			for (Tag t : tagList) {
				int hash = hash(t, round - 1);
				if (hash == i) {
					slotResponse.put(t, hash);
					if (slotInfo[hashLength - 1 - i] == 0) {
						slotInfo[hashLength - 1 - i] = 1;
					}
					// 打印在该slot返回的tagID
					//System.out.println("tagID:" + t.getTagID() + " " + "tagHash:" + t.getPseudoRandomList().get(10 + round - 1) + " " + "selectSlot:" + hash);
				}
			}
			
		}
		
		//System.out.println("slotInfo:");
		for (int i = 0; i < slotInfo.length; i++) {
			//System.out.print(slotInfo[i] + " ");
		}
		
		// R是数组allSlotInfo最右边0的位置
		int R = 0;
		for (int i = slotInfo.length - 1; i > 0; i--) {
			if (slotInfo[i] == 0) {
				R = hashLength - 1 - i;
				break;
			}
		}
		
		//System.out.println();
		//System.out.println("--------");
		// n为估计的标签数量
		int n = (int) (1.2897 * Math.pow(2, R));
		//System.out.println("the estimate tagNum:" + n);
		//System.out.println("--------");
		
		
		for (int i = 0; i < hashLength; i++) {
			List<Tag> taglist = getTag(slotResponse, i);
			// 该slot有冲突
			if (taglist.size() > 1) {
				tagListMap.put(taglist, round + 1);
			}
			// 该slot只有一个标签
			if (taglist.size() == 1) {
				identifiedTag.put(taglist.get(0), round);
				//System.out.println("Tag " + taglist.get(0).getTagID() + " is identified!");
			}
		}
		

		return n;
	}
	
	/**
	 * 根据值返回标签集合
	 * @param map 
	 * @param value
	 * @return 标签集合
	 */
	private static ArrayList<Tag> getTag(Map<Tag, Integer> map, int value) {
		Iterator<Entry<Tag, Integer>> it = map.entrySet().iterator();
		ArrayList<Tag> arr = new ArrayList<>();
		while(it.hasNext()) {
			Entry<Tag, Integer> entry = it.next();
			if (entry.getValue().equals(value)) {
				Tag t = entry.getKey();
				arr.add(t);
			}
		}
		return arr;
	}

	/**
	 * 根据值返回标签集合的集合
	 * @param map
	 * @param value
	 * @return 标签集合的集合
	 */
	private static ArrayList<List<Tag>> getTagList(Map<List<Tag>, Integer> map, int value) {
		Iterator<Entry<List<Tag>, Integer>> it = map.entrySet().iterator();
		ArrayList<List<Tag>> arr = new ArrayList<>();
		while(it.hasNext()) {
			Entry<List<Tag>, Integer> entry = it.next();
			if (entry.getValue().equals(value)) {
				List<Tag> t = entry.getKey();
				arr.add(t);
			}
		}
		return arr;
	}
}

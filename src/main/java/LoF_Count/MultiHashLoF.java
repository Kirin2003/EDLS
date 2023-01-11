package LoF_Count;

import base.Tag;
import utils.Environment;
import utils.Recorder;

import java.util.*;

/**
 * 本类在基础LoF基础上增加了Multi Hash和Sudden Victory机制来使标签的估计值更精确
 * 可以在Main_LoF中更改hashNum来更改MultiHash的次数
 * @author LiMingzhe
 *
 */
public class MultiHashLoF {
	public static int hashStrLength = 20;
	public static int hashNum = 10;

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
		for(Tag tag : tagList) {
			tag.setPseudoRandomList(cidRandomlistMap.get(tag.categoryID));
			tag.setPseudoRanStrLen(hashStrLength);
			tag.setPseudoRanListLen(hashNum);
		}
		return cidRandomlistMap;
	}

	/**
	 * 估计标签数量
	 * @return 估计的数量
	 */
	public static int estimate(List<Tag> tagList) {
		// add
		genMap(tagList);
		// 标签的总值
		int sumN = 0;

		// 获得循环的数量
		int hashNum = tagList.get(0).getPseudoRanListLen();

		// 获得标签hash长度
		int hashLength = tagList.get(0).getPseudoRandomList().get(10).length();

		// 循环hashNum次
		for (int multiHash = 0; multiHash < hashNum; multiHash++) {
			// 总的记录slot信息的数组
			byte[] slotInfo = new byte[hashLength];

			//System.out.println("info of estimation:--------------------" + "the " + (multiHash + 1) + " round");

			// 读取读写器覆盖的标签里的哈希值，用来确定在哪个slot返回
			for (int i = 0; i < hashLength; i++) {
				for (Tag t : tagList) {
					if (hash(t, multiHash) == i) {
						if (slotInfo[hashLength - 1 - i] == 0) {
							slotInfo[hashLength - 1 - i] = 1;
						}
						// 打印在该slot返回的tagID
						//System.out.println("tagID:" + t.getTagID() + " " + "tagHash:" + t.getPseudoRandomList().get(10 + multiHash) + " " + "selectSlot:" + hash(t, multiHash));
					}
				}

				// Sudden Victory机制
				if (slotInfo[hashLength - 1 - i] == 0) {
					//System.out.println("Sudden Victory!");

					break;
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

			sumN += n;
		}


		//System.out.println("--------------------");
		// System.out.println(sumN + " " + hashNum);
		return sumN / hashNum;
	}

	/**
	 * 估计标签数量
	 * @return 估计的数量
	 */
	public static int estimate(List<Tag> tagList, Recorder recorder) {
		// add
		genMap(tagList);
		// 标签的总值
		int sumN = 0;

		// 获得循环的数量,本项目为10
		int hashNum = tagList.get(0).getPseudoRanListLen();

		// 获得标签hash长度,本项目为20
		int hashLength = tagList.get(0).getPseudoRandomList().get(0).length();

		// 总共经历的时隙数,(包括reader query = 4 * slot)
		int slotNum = 0;

		// 循环hashNum次
		for (int multiHash = 0; multiHash < hashNum; multiHash++) {
			// 总的记录slot信息的数组
			byte[] slotInfo = new byte[hashLength];

			//System.out.println("info of estimation:--------------------" + "the " + (multiHash + 1) + " round");

			for(Tag t : tagList) {
				int i = hash(t,multiHash);
				slotInfo[hashLength-i-1]=1;
			}
			for(int i = 0; i < hashLength; i++) {
//				System.out.print(slotInfo[i]);
			}
//			System.out.println();

			int R = 0;
			for(int i = hashLength-1; i >= 0; i--) {
				if(slotInfo[i]== 0) {
					// sudden victory机制
					slotNum += hashLength-i;
					slotNum += 8;
					R = hashLength-1-i;
					break;
				}
			}


			//System.out.println();
			//System.out.println("--------");
			// n为估计的标签数量
			int n = (int) (1.2897 * Math.pow(2, R));

			sumN += n;
		}
		System.out.println("使用LOF估算类别数花费的时间:"+slotNum*0.4+"ms");
		System.out.println("估算的类别为:"+sumN/hashNum);
		recorder.totalExecutionTime+=slotNum*0.4;


		//System.out.println("--------------------");
		// System.out.println(sumN + " " + hashNum);
		return sumN / hashNum;
	}
}

package LoF_Count;

import base.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本类为LoF的实现类，用来估算区域内的标签个数
 * 
 * @author LiMingzhe
 *
 */
public class LoF {

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



	/**
	 * 估计标签数量
	 * 
	 * @return 估计的数量
	 */
	public static int estimate(List<Tag> tagList) {
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

}

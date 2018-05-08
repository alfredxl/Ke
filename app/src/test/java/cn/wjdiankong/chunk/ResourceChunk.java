package cn.wjdiankong.chunk;

import java.util.ArrayList;

import cn.wjdiankong.main.Utils;

public class ResourceChunk {
	
	public byte[] type;
	public byte[] size;
	public byte[] ids;
	
	public ArrayList<Integer> resourcIdList;
	
	public static ResourceChunk createChunk(byte[] byteSrc, int offset){
		
		ResourceChunk chunk = new ResourceChunk();
		
		chunk.type = Utils.copyByte(byteSrc, 0+offset, 4);
		
		chunk.size = Utils.copyByte(byteSrc, 4+offset, 4);
		int chunkSize = Utils.byte2int(chunk.size);
		
		chunk.ids = Utils.copyByte(byteSrc, 8+offset, chunkSize-8);
		
		//������Ҫע�����chunkSize�ǰ�����chunkTag��chunkSize�������ֽڵģ�������Ҫ�޳�
		byte[] resourceIdByte = Utils.copyByte(byteSrc, 8+offset, chunkSize-8);
		ArrayList<Integer> resourceIdList = new ArrayList<Integer>(resourceIdByte.length/4);
		for(int i=0;i<resourceIdByte.length;i+=4){
			int resId = Utils.byte2int(Utils.copyByte(resourceIdByte, i, 4));
			resourceIdList.add(resId);
		}
		
		return chunk;
		
	}

}

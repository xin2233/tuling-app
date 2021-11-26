package com.zjxwwwrobot.robotapp;

public class ListData {
	
	public static final int SEND = 1;
	public static final int RECEIVER = 2;
	private String content; //显示的内容
	private int flag;       // 用来判断 时接受的内容 还是 发送的内容，被send 和receive 进行赋值
	private String time;    //时间
	
	public ListData(String content,int flag,String time) {
		setContent(content);
		setFlag(flag);
		setTime(time);
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
}


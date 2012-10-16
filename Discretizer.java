import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

/*
 *  データの離散化を行うプログラム
 */
public class Discretizer {
	// データセット保持用
	private ArrayList<Data> dataSet = null;
	// 分割点情報保持用
	private ArrayList<Info> splitInfo = null;
	// データセットファイル名
	private String datasetFilename = null;
	// 分割数
	private int binNum = 0;
	// アルゴリズム名
	private String algorithm = null;
	// CSV出力用
	private BufferedWriter out = null;
	
	/* 
	 * コンストラクタ
	 * データセットの読み込みと初期設定を行う．
	 * @param 
	 * dataset_filename データセットのファイルパス(CSV形式=> value,label)
	 * num_of_bin      分割数
	 * algorithm       アルゴリズム名
	 */
	public Discretizer(String dataset_filename, String num_of_bin, String algorithm){
		dataSet = new ArrayList<Data>();
		if( algorithm.equals("Equal-Width") || algorithm.equals("Equal-Depth") || algorithm.equals("Entropy-Based") || algorithm.equals("Entropy-Minimization")){
			this.algorithm = algorithm;
		}else{
			System.err.println("No match algorithm : "+algorithm);
			System.out.println("ALGORITHM => Equal-Width, Equal-Depth, Entropy-Based, Entropy-Minimization");
			System.exit(-1);
		}
		try{
			binNum = Integer.parseInt(num_of_bin);
			if( binNum == 0 ){
				System.err.println("Bin Num must be positive integer!!");
				System.exit(-1);
			}
		}catch( NumberFormatException e){
			System.err.println("Bin Num must be Integer!!");
			System.exit(-1);
		}
		try{
			datasetFilename = dataset_filename;
			BufferedReader br = new BufferedReader(new FileReader(datasetFilename));
			String line = null;
			while( (line = br.readLine()) != null ){
				try{
					String[] sp = line.split(",");
					dataSet.add(new Data(Double.parseDouble(sp[0]),sp[1]));
				}catch( NumberFormatException e){
					System.err.println("Data Format ERROR!!");
					System.exit(-1);
				}
			}
			br.close();
		}catch( IOException e) {
			System.err.println("IOException at reading dataset file!!");
			System.exit(-1);
		}
		splitInfo = new ArrayList<Info>();
		try {
			out = new BufferedWriter(new FileWriter(this.algorithm+".csv"));
		} catch (IOException e) {
			System.err.println("Cannot Open Output File...");
			System.exit(-1);
		}
	}
	
	/*
	 * 離散化実行関数
	 */
	public void run(){
		if( this.algorithm.equals("Equal-Width") )
			Equal_Width();
		else if( this.algorithm.equals("Equal-Depth") )
			Equal_Depth();
		else if( this.algorithm.equals("Entropy-Based") )
			Entropy_Based();
		else if( this.algorithm.equals("Entropy-Minimization") )
			Entropy_Minimization();
		else{
			System.err.println("No match algorithm : "+this.algorithm);
			System.exit(-1);
		}
		print();
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Equal-Width
	 */
	private void Equal_Width(){
		Collections.sort(dataSet);
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		for( int i=0; i<dataSet.size(); i++ ){
			if( dataSet.get(i).getVal() > max )
				max = dataSet.get(i).getVal();
			if( dataSet.get(i).getVal() < min )
				min = dataSet.get(i).getVal();
		}
		double spLength = (max-min)/binNum;
		double spp = dataSet.get(0).getVal()+spLength;
		int spNo = 1;
		for( int i=0; i<dataSet.size(); i++ ){
			if( dataSet.get(i).getVal() > spp ){
				splitInfo.add(new Info(spp,0.0));
				spp += spLength;
				++spNo;
			}
			dataSet.get(i).setBin(spNo);
		}
	}
	
	/*
	 * Equal-Depth
	 */
	private void Equal_Depth(){
		Collections.sort(dataSet);
		int dataNum = dataSet.size();
		double numPerClus = (double)dataNum/binNum;
		int spNo = 1;
		double spcnt = numPerClus;
		for( int i=0; i<dataSet.size(); i++){
			if( (i+1) > spcnt && (spNo+1) <= binNum ){
				splitInfo.add(new Info((dataSet.get(i).getVal()+dataSet.get(i-1).getVal())/2,0.0));
				spcnt += numPerClus;
				++spNo;
			}
			dataSet.get(i).setBin(spNo);
		}
	}
	
	/*
	 * Entropy-Based
	 */
	private void Entropy_Based(){
		Collections.sort(dataSet);
		Queue<Pair> queue = new LinkedList<Pair>();
		queue.add(new Pair(0,dataSet.size()));
		while(true){
			if( queue.isEmpty())
				break;
			Pair pair = queue.poll();
			double min_info = Double.MAX_VALUE;
			int spp = -1;
			CalcInfo info = null, tmpInfo = null;
			for( int i=pair.getStart()+1; i<pair.getEnd(); i++){
				int sp = i;
				info=null;
				if( dataSet.get(sp-1).getVal() != dataSet.get(sp).getVal() )
					info = calcInfo(pair.getStart(), pair.getEnd(), dataSet.get(sp).getVal());
				if( info != null && min_info > info.getInfo() ){
					min_info = info.getInfo();
					spp = i;
					tmpInfo = info;
				}
			}
			if( spp !=  -1  && tmpInfo != null ){
				splitInfo.add(new Info((dataSet.get(spp-1).getVal()+dataSet.get(spp).getVal())/2, min_info));
				if( splitInfo.size() >= binNum-1 )
					break;
				if( tmpInfo.getLeft() != 0.0 )
					queue.add(new Pair(pair.getStart(),spp));
				if( tmpInfo.getRight() != 0.0 )
					queue.add(new Pair(spp,pair.getEnd()));
			}
		}
		Collections.sort(splitInfo);
		int cnt = 0;
		int spNo = 1;
		for( int i=0; i<dataSet.size(); i++){
			if( cnt <= splitInfo.size()-1 && splitInfo.get(cnt).getSp() < dataSet.get(i).getVal() ){
				++cnt;
				++spNo;
			}
			dataSet.get(i).setBin(spNo);
		}
	}
	
	/*
	 * Entropy-Minimization
	 */
	private void Entropy_Minimization(){
		for( int i=1; i<dataSet.size(); i++)
			if( (dataSet.get(i-1).getLabel().equals("No") && dataSet.get(i).getLabel().equals("Yes")) || (dataSet.get(i-1).getLabel().equals("Yes") && dataSet.get(i).getLabel().equals("No") ))
				splitInfo.add(new Info((dataSet.get(i-1).getVal()+dataSet.get(i).getVal())/2,0));
		for( int i=0; i<splitInfo.size(); i++)
			splitInfo.get(i).setInfo((calcInfo(0, dataSet.size(), splitInfo.get(i).getSp())).getInfo());
		Collections.sort(splitInfo, new Comparator<Object>(){
			public int compare(Object obj1, Object obj2){
				return (int)(((Info)obj1).getInfo()*1000-((Info)obj2).getInfo()*1000);
			}
		});
		ArrayList<Info> tmp = new ArrayList<Info>();
		for( int k=0; k<binNum-1; k++)
			if( k < splitInfo.size())
				tmp.add(splitInfo.get(k));
		Collections.sort(tmp);
		int cnt = 0;
		int spNo = 1;
		for( int i=0; i<dataSet.size(); i++){
			if(  cnt < tmp.size() && tmp.get(cnt).getSp() < dataSet.get(i).getVal() ){
				++cnt;
				++spNo;
			}
			dataSet.get(i).setBin(spNo);
		}
		splitInfo = tmp;
		Collections.sort(splitInfo);
	}
	
	/*
	 * Entropy-BasedとEntropy-Minimizationにおいて情報量の計算を行う
	 * @param
	 * start データセットにおいて情報量を求める要素の開始インデックス
	 * end   データセットにおいて情報量を求める要素の終了インデックス
	 * sp    情報量を求める際のデータセットにおける分割点(start~endまでの範囲にあること!)
	 */
	private CalcInfo calcInfo(int start, int end, double sp){
		double[] left = new double[2], right = new double[2];
		for(int j=start; j<end; j++){
			if( dataSet.get(j).getVal() < sp ){
				if( dataSet.get(j).getLabel().equals("Yes") )
					++left[0];
				else
					++left[1];
			}else{
				if( dataSet.get(j).getLabel().equals("Yes") )
					++right[0];
				else
					++right[1];
			}
		}
		
		double info_left = -(left[0]/(left[0]+left[1]))*Math.log(left[0]/(left[0]+left[1]))-(left[1]/(left[0]+left[1]))*Math.log(left[1]/(left[0]+left[1]));
		double info_right= -(right[0]/(right[0]+right[1]))*Math.log(right[0]/(right[0]+right[1]))-(right[1]/(right[0]+right[1]))*Math.log(right[1]/(right[0]+right[1]));
		if( Double.isNaN(info_left))
			info_left = 0.0;
		if( Double.isNaN(info_right))
			info_right = 0.0;
		double total = left[0]+left[1]+right[0]+right[1];
		try {
			out.write(start+","+end+","+sp+","+(((left[0]+left[1])/total)*info_left+((right[0]+right[1])/total)*info_right)+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (new CalcInfo(info_left, info_right, ((left[0]+left[1])/total)*info_left+((right[0]+right[1])/total)*info_right));
	}

	/*
	 * 分割結果出力関数
	 */
	private void print(){
		System.out.println("***SPLIT POINTS***");
		System.out.println("#split\t#info_value");
		for( int i=0; i<splitInfo.size(); i++)
			System.out.println(splitInfo.get(i).getSp()+"\t"+splitInfo.get(i).getInfo());
		System.out.println("\n***Discretization Data***");
		System.out.println("#value\t#bin\t#label");
		for( int i=0; i<dataSet.size(); i++)
			System.out.println(dataSet.get(i).getVal()+"\t"+dataSet.get(i).getBin()+"\t"+dataSet.get(i).getLabel());
	}
	
	/*
	 * メイン関数
	 * @param
	 * args[0] データセットファイルパス
	 * args[1] 分割数
	 * args[2] アルゴリズム名
	 */
	public static void main(String[] args){
		
		if( args.length != 3 ){
			System.err.println("###ERROR### Invalid arguments!");
			System.out.println("Usage => java Discretization 'dataset_filename' 'num_of_bin' 'algorithm_name'");
			System.exit(-1);
		}
		
		Discretizer dis = new Discretizer(args[0], args[1], args[2]);
		dis.run();
	}

}

/*
 * データ保持用エレメント
 */
class Data implements Comparable<Object>{
	
	private double val;
	private String label;
	private int bin;
	
	public Data(double val, String label){
		this.val = val;
		this.label = label;
		this.bin = 0;
	}
	
	public void setBin(int bin){
		this.bin = bin;
	}
	
	public int getBin(){
		return this.bin;
	}
	
	public double getVal(){
		return this.val;
	}
	
	public String getLabel(){
		return this.label;
	}

	@Override
	public int compareTo(Object o) {
		Data obj = (Data)o;
		return (int)(this.val-obj.getVal());
	}
}

/*
 * 分割点の情報保持用エレメント
 */
class Info implements Comparable<Object>{
	private double sp=0.0;
	private double info=0.0;
	
	public Info(double sp, double info){
		this.sp = sp;
		this.info = info;
	}
	
	public double getSp(){
		return this.sp;
	}
	
	public double getInfo(){
		return this.info;
	}
	
	public void setInfo(double info){
		this.info = info;
	}

	@Override
	public int compareTo(Object arg0) {
		Info obj = (Info)arg0;
		return (int)(this.sp-obj.getSp());
	}
}

/*
 * 情報量を求める際の区間指定用エレメント
 */
class Pair {
	
	private int start=0;
	private int end=0;
	
	public Pair(int start, int end){
		this.start = start;
		this.end = end;
	}
	
	public int getStart(){
		return this.start;
	}
	
	public int getEnd(){
		return this.end;
	}
}

/*
 * calcInfoの返り値用クラス
 */
class CalcInfo{
	
	private double left;
	private double right;
	private double info;
	
	public CalcInfo(double left, double right, double info){
		this.left = left;
		this.right = right;
		this.info = info;
	}

	public double getLeft(){
		return this.left;
	}
	
	public double getRight(){
		return this.right;
	}
	
	public double getInfo(){
		return this.info;
	}
}

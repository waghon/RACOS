/**
 * Class continue
 * @author Yi-Qi Hu
 * @time 2015.11.14
 * @version 2.0
 * continue class is the method of Racos in continuous feasible region
 */

package Racos.Method;

import Racos.Componet.Dimension;
import Racos.Tools.*;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import Racos.Componet.*;
import Racos.ObjectiveFunction.*;

public class Continue extends BaseParameters{
	
	private Task task;                   //the task that algorithm will optimize       
	private Dimension dimension;         //the task dimension message that algorithm optimize
	private Instance[] Pop;              //the instance set that algorithm will handle with
	private Instance[] NextPop;          //the instance set that algorithm will generate using instance set Pop
	private Instance[] PosPop;           //the instance set with best objective function value
	private Instance Optimal;            //an instance with the best objective function value
	private LinkedList<Instance> savedSamples;		//the size for sample to save
	private boolean on_off;				 //switch of sequential racos
	private int BudCount;
	private Model model;
	private RandomOperator ro;
	private double gradient;
	private int savedSampleSize;
	boolean isPosPopChanged = false;
	private int optimalNotChangedTurns = 0;
	private int maxNotChangedTurns;
	private int current_Turn;


	private class Model{                 //the model of generating next instance
		
		public double[][] region;//shrinked region
		public boolean[] label;  //if label[i] is false, the corresponding dimension should be randomized from region
		
		public Model(int size){
			region = new double[size][2];
			label = new boolean[size];
			for(int i=0; i<size; i++){
				region[i][0] = 0;
				region[i][1]  =1;
				label[i] = false;
			}
		}
		
		public void PrintLabel(){
			for(int i=0; i<label.length; i++){
				if(!label[i]){
					System.out.print(i+" ");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * constructors function
	 * user must construct class Continue with a class which implements interface Task
	 * 
	 * @param ta  the class which implements interface Task
	 */
	public Continue(Task ta){
		task = ta;
		dimension = ta.getDim();
		ro = new RandomOperator();
		this.on_off = false; 			//set batch-racos
	}
	
	//the next several functions are prepared for testing
	public void PrintPop(){
		System.out.println("Pop set:");
		if(Pop.length==0){
			System.out.println("Pop set is null!");
		}
		for(int i=0; i<Pop.length; i++){
			Pop[i].PrintInstance();
		}
	}
	public void PrintPosPop(){
		System.out.println("PosPop set:");
		if(PosPop.length==0){
			System.out.println("PosPop set is null!");
		}
		for(int i=0; i<PosPop.length; i++){
			PosPop[i].PrintInstance();
		}
	}
	public void PrintNextPop(){
		System.out.println("NextPop set:");
		if(NextPop.length==0){
			System.out.println("NextPop set is null!");
		}
		for(int i=0; i<NextPop.length; i++){
			NextPop[i].PrintInstance();
		}
	}
	public void PrintOptimal(){
		System.out.println("Optimal:");
		Optimal.PrintInstance();
	}
	public void PrintRegion(){
		System.out.println("Region:");
		for(int i=0; i<model.region.length; i++){
			System.out.print("["+model.region[i][0]+","+model.region[i][1]+"] ");
		}
		System.out.println();
	}
	
	/**
	 * using sequential Racos
	 */
	public void TurnOnSequentialRacos(){
		this.on_off = true;
		return ;
	}
	
	/**
	 * using batch-Racos
	 */
	public void TurnOffSequentialRacos(){
		this.on_off = false;
		return ;
	}
	
	/**
	 * 
	 * @return the optimal that algorithm found
	 */
	public Instance getOptimal(){
		return Optimal;
	}
	
	/**
	 * RandomInstance without parameter
	 * 
	 * @return an Instance, each feature in this instance is a random number from original feasible region
	 */
	protected Instance RandomInstance(){
		Instance ins = new Instance(dimension);
		for(int i=0; i<dimension.getSize(); i++){
			
			if(dimension.getType(i)){//if i-th dimension type is continue
				ins.setFeature(i, ro.getDouble(dimension.getRegion(i)[0], dimension.getRegion(i)[1]));
			}else{//if i-th dimension type is discrete
				ins.setFeature(i, ro.getInteger((int)dimension.getRegion(i)[0], (int)dimension.getRegion(i)[1]));
			}
			
		}
		return ins;
	}
	
	/**
	 * Random instance with parameter
	 * 
	 * @param pos, the positive instance that generate the model
	 * @return an Instance, each feature in this instance is a random number from a feasible region named model
	 */
	protected Instance RandomInstance(Instance pos){
		Instance ins = new Instance(dimension);
//		model.PrintLabel();
		for(int i=0; i<dimension.getSize(); i++){
			
			if(dimension.getType(i)){//if i-th dimension type is continue
				if(model.label[i]){//according to fixed dimension, valuate using corresponding value in pos
					ins.setFeature(i, pos.getFeature(i));
				}else{//according to not fixed dimension, random in region
//					System.out.println("["+model.region[i][0]+", "+model.region[i][1]+"]");
					ins.setFeature(i, ro.getDouble(model.region[i][0], model.region[i][1]));
				}
			}else{//if i-th dimension type is discrete
				if(model.label[i]){//according to fixed dimension, valuate using corresponding value in pos
					ins.setFeature(i, pos.getFeature(i));
				}else{//according to not fixed dimension, random in region
					ins.setFeature(i, ro.getInteger((int)model.region[i][0], (int)model.region[i][1]));
				}
			}
			
		}
		return ins;
	}
	
	/**
	 * initialize Pop, NextPop, PosPop and Optimal
	 * 
	 */
	@SuppressWarnings("unchecked")
	protected void Initialize(){
		
		Instance[] temp = new Instance[SampleSize+PositiveNum];
		
		Pop = new Instance[SampleSize];
		
		//sample Sample+PositiveNum instances and add them into temp
		for(int i=0; i<SampleSize+PositiveNum; i++){
			temp[i] = RandomInstance();
			temp[i].setValue(task.getValue(temp[i]));
			if (this.on_off){
				this.BudCount++;
			}
		}	
				
		//sort Pop according to objective function value
		InstanceComparator comparator = new InstanceComparator();
		java.util.Arrays.sort(temp,comparator);
		
		//initialize Optimal
		Optimal = temp[0].CopyInstance();
		
		//after sorting, the beginning several instances in temp are used for initializing PosPop
		PosPop = new Instance[PositiveNum];
		for(int i=0; i<PositiveNum; i++){
			PosPop[i] = temp[i];
		}
		
		Pop = new Instance[SampleSize];
		for(int i=0; i<SampleSize; i++){
			Pop[i] = temp[i+PositiveNum];
		}

		savedSamples = new LinkedList<Instance>();
		for(int i=0;i<savedSampleSize;i++){
			Instance newSample = RandomInstance();
			newSample.setValue(task.getValue(newSample));
			savedSamples.add(newSample);
		}

		//initialize NextPop
		NextPop = new Instance[SampleSize];
		
		model = new Model(dimension.getSize());
		
		return ;
		
	}
	
	/**
	 * reset sampling model
	 * @return the model with original feasible region and all label is true
	 */
	protected void ResetModel(){
		for(int i=0; i<dimension.getSize(); i++){
			model.region[i][0] = dimension.getRegion(i)[0];	
			model.region[i][1] = dimension.getRegion(i)[1];	
			model.label[i] = false;
		}
		return ;
	}
	
	/**
	 * shrink model for instance pos
	 * 
	 * @param pos
	 */
	protected void ShrinkModel(Instance pos){
		int ChoosenDim;  
		int ChoosenNeg;  
		double tempBound;
		
		int ins_left = SampleSize;
		while (ins_left>0) {//generate the model
			
			ChoosenDim = ro.getInteger(0, dimension.getSize() - 1);//choose a dimension randomly
			ChoosenNeg = ro.getInteger(0, this.SampleSize - 1);    //choose a negative instance randomly
			// shrink model
			if (pos.getFeature(ChoosenDim) < Pop[ChoosenNeg].getFeature(ChoosenDim)) {
				tempBound = ro.getDouble(pos.getFeature(ChoosenDim), Pop[ChoosenNeg].getFeature(ChoosenDim));
				if (tempBound < model.region[ChoosenDim][1]) {
					model.region[ChoosenDim][1] = tempBound;
					int i=0;
					while(i<ins_left){
						if(Pop[i].getFeature(ChoosenDim)>=tempBound){
							ins_left--;
							Instance tempins = Pop[i];
							Pop[i] = Pop[ins_left];
							Pop[ins_left] = tempins;
						}else{
							i++;
						}
					}

				}
			} else {
				tempBound = ro.getDouble(Pop[ChoosenNeg].getFeature(ChoosenDim), pos.getFeature(ChoosenDim));
				if (tempBound > model.region[ChoosenDim][0]) {
					model.region[ChoosenDim][0] = tempBound;
					int i=0;
					while(i<ins_left){
						if(Pop[i].getFeature(ChoosenDim)<=tempBound){
							ins_left--;
							Instance tempins = Pop[i];
							Pop[i] = Pop[ins_left];
							Pop[ins_left] = tempins;
						}else{
							i++;
						}
					}
				}
			}
		}
		return ;
		
	}
	
	/**
	 * make sure that the number of random dimension is smaller than the threshold UncertainBits
	 * 
	 * @param model
	 * @return
	 */
	protected void setRandomBits(){
		int labelMarkNum;
		int[] labelMark = new int[dimension.getSize()];
		int tempLab;
		labelMarkNum = dimension.getSize();
		for(int k=0; k<dimension.getSize(); k++){
			labelMark[k] = k;
		}
		for (int k = 0; k < dimension.getSize()-UncertainBits; k++) {						
			tempLab = ro.getInteger(0, labelMarkNum-1);
			model.label[labelMark[tempLab]] = true;
			labelMark[tempLab] = labelMark[labelMarkNum-1];
			labelMarkNum--;
		}
		return ;
		
	}
	
	/**
	 * if each instance in Pop is not in model, return true; if exist more than one instance in Pop is in the model, return false
	 * 
	 * @param model
	 * @return true or false
	 */
	protected boolean Distinguish(){
		int j;
		for(int i=0;i<this.SampleSize; i++){
			for(j=0; j<dimension.getSize(); j++){
				if(Pop[i].getFeature(j)>model.region[j][0]&&Pop[i].getFeature(j)<model.region[j][1]){
					
				}else{
					break;
				}
			}
			if(j==dimension.getSize()){
				return false;
			}
		}
		return true;		
	}
	
	/**
	 * judge whether the instance is in the region of model, if instance is in the model, return true, else return true
	 * @param model, the model
	 * @param ins, the instance
	 * @return
	 */
	protected boolean InstanceIsInModel(Model model, Instance ins){
		for(int i=0; i<ins.getFeature().length; i++){
			if(ins.getFeature(i)>model.region[i][1]||ins.getFeature(i)<model.region[i][0]){
				return false;//if i-th dimension is not in the region, return false
			}
		}
		return true;
	}
	
	/**
	 * if ins exist in Pop, PosPop, NextPop, return true; else return false
	 * @param ins
	 * @return
	 */
	protected boolean notExistInstance(int end, Instance ins){
		int i,j;
		for(i=0; i<this.PositiveNum;i++){
			if(ins.Equal(PosPop[i])){
				return false;
			}
		}
		for(i=0;i<this.SampleSize;i++){
			if(ins.Equal(Pop[i])){
				return false;
			}
		}
		for(i=0;i<end;i++){
			if(ins.Equal(NextPop[i])){
				return false;
			}
		}
		return true;
		
	}
	
	/**
	 * update set PosPop using NextPop
	 * 
	 */
	protected void UpdatePosPop(){
		Instance TempIns = new Instance(dimension);
		int j;
		for(int i=0; i<this.SampleSize; i++){
			for(j=0; j<this.PositiveNum; j++){
				if(PosPop[j].getValue()>Pop[i].getValue()){
					break;
				}
			}
			if(j<this.PositiveNum){
				TempIns=Pop[i];
				Pop[i]=PosPop[this.PositiveNum-1];
				for(int k=this.PositiveNum-1; k>j;k--){
					PosPop[k]=PosPop[k-1];
				}
				PosPop[j]=TempIns;
			}
		}
		return ;
	}
	
	/**
	 * update sample set for sequential racos
	 * 
	 */
	protected void UpdateSampleSet(Instance temp){
		Instance TempIns = new Instance(dimension);
		RandomOperator ro = new RandomOperator();
		int j;
		
		for(j=0; j<this.PositiveNum; j++){
			if(PosPop[j].getValue()>temp.getValue()){
				break;
			}
		}
		if(j<this.PositiveNum){
			TempIns=temp;
			temp=PosPop[this.PositiveNum-1];
			for(int k=this.PositiveNum-1; k>j;k--){
				PosPop[k]=PosPop[k-1];
			}
			PosPop[j]=TempIns;
			isPosPopChanged = true;
		}
		else{
			isPosPopChanged = false;
		}

		for(j=0; j<this.SampleSize; j++){
			if(Pop[j].getValue()>temp.getValue())
				break;
		}
		if(j<this.SampleSize){
			for(int k=this.SampleSize-1; k>j;k--){
				Pop[k]=Pop[k-1];
			}
			Pop[j]=temp;
		}
		return ;
	}
	
	/**
	 * update optimal
	 */
	protected void UpdateOptimal(){
		if (Optimal.getValue() > PosPop[0].getValue()) {
			Optimal=PosPop[0];			
		}
		return ;
	}
	
	/**
	 * after setting parameters of Racos, user call this function can obtain optimal
	 * 
	 */
	public void run(){
		
		this.BudCount = 0;
		int ChoosenPos;
		double GlobalSample;
		boolean reSample;
		/////////////////////////////////////
		double[] result = new double[6];
		
		model = new Model(dimension.getSize());
		
		ResetModel();
		Initialize();
		
		// batch Racos
		if (!this.on_off){
			// for each loop
			for(int i=1; i<this.MaxIteration; i++){	
				// for each sample in a loop
				for(int j=0; j<this.SampleSize; j++){	
					reSample = true;
					while (reSample) {		
						ResetModel();
						ChoosenPos = ro.getInteger(0, this.PositiveNum - 1);
						GlobalSample = ro.getDouble(0, 1);
						if (GlobalSample >= this.RandProbability) {
						} else {	
							ShrinkModel(PosPop[ChoosenPos]);//shrinking model
							setRandomBits();//set uncertain bits							
						}
						NextPop[j] = RandomInstance(PosPop[ChoosenPos]);//sample
						if (notExistInstance(j, NextPop[j])) {
							NextPop[j].setValue(task.getValue(NextPop[j]));//query
							reSample = false;
						}
					}
				}				
				//copy NextPop
				for (int j = 0; j < this.SampleSize; j++) {
					Pop[j] = NextPop[j];
				}				
				UpdatePosPop();//update PosPop set				
				UpdateOptimal();//obtain optimal	
			}
		}else{
			// sequential Racos
			Instance new_sample = null;
			double sampleValues[] = new double[this.Budget];
			double sampleTrueValues[] = new double[this.Budget];
			for(; BudCount<this.Budget; ){
				reSample = true;
				while (reSample) {
					ResetModel();
					ChoosenPos = ro.getInteger(0, this.PositiveNum - 1);
					GlobalSample = ro.getDouble(0, 1);
					if (GlobalSample >= this.RandProbability) {
					} else {
						ShrinkModel(PosPop[ChoosenPos]);//shrinking model
						setRandomBits();//set uncertain bits
					}
					new_sample = RandomInstance(PosPop[ChoosenPos]);//sample
					if (notExistInstance(0, new_sample)) {
						new_sample.setValue(task.getValue(new_sample));//query
						//reviseInstance(PosPop[ChoosenPos],new_sample);
						BudCount++;
						reSample = false;
						//System.out.println(PosPop[ChoosenPos].getFeature() +": "+PosPop[ChoosenPos].getTrueValue()+ ": "+PosPop[ChoosenPos].getValue());
					}
				}
				UpdateSampleSet(new_sample);//update PosPop set
				UpdateOptimal();//obtain optimal
				UpdateSavedSamples(new_sample); // update the samples to save
				checkOptimal();	//check if the optimal is normal
				sampleValues[BudCount-1] = Optimal.getValue();
				sampleTrueValues[BudCount-1] = task.getTrueValue(Optimal);
			}
			//saveToFile(sampleValues,"sampleValues"+current_Turn+".txt");
			saveToFile(sampleTrueValues,"sampleTrueValuesRevised"+current_Turn+".txt");
			//saveToFile(sampleTrueValues,"sampleTrueValues"+current_Turn+".txt");
			//saveToFile(sampleTrueValues,"sampleTrueValuesOnePos"+current_Turn+".txt");
		}
		return ;		
	}


	private void checkOptimal() {
		if(isPosPopChanged){
			optimalNotChangedTurns = 0;
			return;
		}
		optimalNotChangedTurns ++;
		if(optimalNotChangedTurns == maxNotChangedTurns){
			for(int i=0;i<PositiveNum;i++){
				PosPop[i].setValue(PosPop[i].getValue()+ 0.05*task.getGaussNoiseSigma());
			}
			//System.out.println("changed");
			for(Instance ins:savedSamples){
				//UpdatePosPop(ins);
			}
			optimalNotChangedTurns = 0;
		}
	}

	private void UpdatePosPop(Instance temp) {
		Instance TempIns = new Instance(dimension);
		int j;
		for(j=0; j<this.PositiveNum; j++){
			if(PosPop[j].getValue()>temp.getValue()){
				break;
			}
		}
		if(j<this.PositiveNum){
			TempIns=temp;
			temp=PosPop[this.PositiveNum-1];
			for(int k=this.PositiveNum-1; k>j;k--){
				PosPop[k]=PosPop[k-1];
			}
			PosPop[j]=TempIns;
			isPosPopChanged = true;
		}
		else{
			isPosPopChanged = false;
		}
	}

	/**
	 * save data to file
	 */
	private void saveToFile(double[] values, String fileName) {
		PrintWriter fileOut = null;
		try {
			fileOut = new PrintWriter(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int size = values.length;
		for(int i=0;i<size;i++){
			fileOut.println(values[i]);
		}
		fileOut.close();
	}

	/**
	 * use 500 instances to approximate the gradient
	 */
	public void setGradient(){
		int trainDataSize = 500;
		Instance[] trainData = new Instance[trainDataSize];
		for(int i = 0;i < trainDataSize;i++){
			trainData[i] = RandomInstance();
			trainData[i].setValue(task.getValue(trainData[i]));
		}
		double sumDistance = 0;
		double sumValueDiff = 0;
		double sumGradient = 0;
		int count = 0;
		double maxGradient = 0;
		for(int i = 0; i < trainDataSize; i++){
			for(int j = i+1; j < trainDataSize; j++){
				//sumDistance  += countDistance(trainData[i], trainData[j]);
				//sumValueDiff += Math.abs(trainData[i].getValue()-trainData[j].getValue());
				double disrance = countDistance(trainData[i], trainData[j]);
				double valueDiff = Math.abs(trainData[i].getValue()-trainData[j].getValue());
				sumGradient += valueDiff/disrance;
				if(valueDiff/disrance > maxGradient)
					maxGradient = valueDiff/disrance;
				count ++;
			}
		}
		//gradient = sumGradient/(count);
		gradient = maxGradient;
		System.out.println(gradient);
	}

	/**
	 * count the distance of a and b
	 * @param a instance a
	 * @param b instance b
	 * @return the distance between a and b
	 */
	private double countDistance(Instance a, Instance b){
		int size = dimension.getSize();
		double sum = 0;
		for(int i = 0; i < size; i++){
			sum += Math.pow(Math.abs(a.getFeature(i)-b.getFeature(i)),2);
		}
		return Math.sqrt(sum);
	}

	/**
	 * adverse newSample according to its parents
	 * @param parent
	 * @param child
	 */
	void reviseInstance(Instance parent, Instance child){
		/**
		 * revise child just according to its parent
		 */
		/*double P_value = parent.getValue();
		double C_value = child.getValue();
		double expectedDiff = countDistance(parent,child)*gradient;
		if(expectedDiff + 1*task.getGaussNoiseSigma() < Math.abs(P_value - C_value)){
			//System.out.println("revised: "+ C_value +"  " +child.getTrueValue() +"  "+P_value +"  " +
			//		task.getValue(parent) +"  "+(expectedDiff+Instance.getGaussNoiseSigma()));
			if(C_value < P_value){
				child.setValue(P_value - expectedDiff);
			}
			else{
				//child.setValue(P_value + expectedDiff);
			}
			//System.out.println(child.getValue());
		}*/

		/**
		 * part two, use same samples to revise the child
		 */
		double sumExpectedValue = 0;
		double C_value = child.getValue();
		double originC_Value = C_value;
		int neighborSize = 100;
		Instance neighborIns[] = new Instance[neighborSize];
		for(int i=0;i<neighborSize;i++){
			ResetModel();
			double GlobalSample = ro.getDouble(0, 1);
			if (GlobalSample >= this.RandProbability) {
			} else {
				ShrinkModel(child);//shrinking model
				setRandomBits();//set uncertain bits
			}
			neighborIns[i] = RandomInstance(child);
			neighborIns[i].setValue(task.getValue(neighborIns[i]));
		}
 		for(int i=0;i<neighborSize;i++){
			/*double sampleValue = neighborIns[i].getValue();
			double expectedDiff = gradient * countDistance(neighborIns[i], child);
			if(C_value < sampleValue) {
				sumExpectedValue += (sampleValue - expectedDiff);
				//System.out.println(sampleValue - expectedDiff+"   dis:"+countDistance(neighborIns[i],child));
			}
			else{
				sumExpectedValue += (sampleValue + expectedDiff);
				//System.out.println(sampleValue + expectedDiff+"   dis:"+countDistance(neighborIns[i],child));
			}*/
			double sampleValue = neighborIns[i].getValue();
			if(Math.abs(sampleValue-C_value)>=gradient*countDistance(neighborIns[i],child)){
				double C_new_value;
				if(C_value > sampleValue){ //going to make C_value smaller
					C_new_value = (sampleValue + gradient*countDistance(neighborIns[i],child));
					C_new_value = Math.max(C_new_value, originC_Value - task.getGaussNoiseSigma());
				}
				else{ //going to make C_value larger
					C_new_value = (sampleValue - gradient*countDistance(neighborIns[i],child));
					C_new_value = Math.min(C_new_value, originC_Value + task.getGaussNoiseSigma());
				}
				child.setValue(C_new_value);
				C_value = child.getValue();
			}
		}
		//System.out.println(originC_Value+": "+task.getTrueValue(child)+": "+ C_value);
		/*double expectedValue = sumExpectedValue/neighborSize;
		System.out.println(expectedValue+": "+C_value+" : "+task.getTrueValue(child));
		if(Math.abs(expectedValue-C_value) > task.getGaussNoiseSigma()){
			child.setValue(expectedValue);
		}*/
	}

	public void setSavedSampleSize(int savedSampleSizeToset) {
		savedSampleSize = savedSampleSizeToset;
		maxNotChangedTurns = savedSampleSizeToset;
	}

	private void UpdateSavedSamples(Instance new_sample) {
		savedSamples.removeFirst();
		savedSamples.add(new_sample);
	}

	public void setCurrentTurn(int turn){
		current_Turn = turn;
	}

}

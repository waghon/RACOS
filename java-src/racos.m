size = 2000000;

totalValue=zeros(size,1);
for i=0:9
    %values=load(['sampleValues]',i,'.txt']);
    trueValues=load(['sampleTrueValuesOnePos',num2str(i),'.txt']);
    totalValue=totalValue+trueValues;
end
totalValue=totalValue/10;
scale=1:1:size;
plot(scale,totalValue);
hold on;

totalValue=zeros(size,1);
for i=0:9
    %values=load(['sampleValues]',i,'.txt']);
    trueValues=load(['sampleTrueValues',num2str(i),'.txt']);
    totalValue=totalValue+trueValues;
end
totalValue=totalValue/10;
scale=1:1:size;
plot(scale,totalValue);
hold on;

totalValue=zeros(size,1);
for i=0:9
    %values=load(['sampleValues]',i,'.txt']);
    trueValues=load(['sampleTrueValuesRevised',num2str(i),'.txt']);
    totalValue=totalValue+trueValues;
end
totalValue=totalValue/10;
scale=1:1:size;
plot(scale,totalValue);
hold on;
legend('PosPop:1','修改前','修改后');
title('Ackley')
axis([1,size,0,0.3]);
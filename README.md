Discretizer
===========

1次元のラベル付き数値データを4つのアルゴリズムで離散化するプログラム  
  
使い方  
コンパイル  
javac Discretizer.java  
java Discretizer [データファイル名] [分割数] [アルゴリズム名]  
  
データファイル形式  
value,label形式。labelはYes/No (サンプルデータファイル参照)  
  
分割数  
最大分割数を指定します。  
  
アルゴリズム  
次の4つのうちから1つ指定します。  
1. Equal-Width : 入力値の範囲を均等に分割します。  
2. Equal-Depth : 各ビンに同数含まれるように分割します。  
3. Entropy-Based : 分割した際のエントロピーが小さいところから順に分割します。  
4. Entropy-Minimization : 最終的なエントロピーが最小になるように分割します。  
  
結果は標準出力に出力されます。  
(出力されるcsvファイルはdebug用です。)  


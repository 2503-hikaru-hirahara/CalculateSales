package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "ファイルのフォーマットが不正です";
	private static final String FILE_NOT_EXIST_RCD = "売上ファイル名が連番になっていません";
	private static final String FILE_INVALID_FORMAT_RCD = "のフォーマットが不正です";
	private static final String VALUE_INVALID_FORMAT_SALEAMOUNT = "合計金額が10桁を超えました";
	private static final String VALUE_NOT_EXIST_BRANCH_CODE = "の支店コードが不正です";
	private static final String VALUE_NOT_EXIST_COMMODITY_CODE = "の商品コードが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		//コマンドライン引数が渡されているかチェック
		if (args.length != 1) {
		    //コマンドライン引数が1つ設定されていなかった場合は、
		    //エラーメッセージをコンソールに表示します。
			System.out.println(UNKNOWN_ERROR);
			return;
		}
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 商品コードと支店名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();
		// 支店コードの正規表現
		String branchRegEx = "[0-9]{3}";
		// 商品コードの正規表現
		String commodityRegEx = "[A-Za-z0-9]{8}";
		// ファイルの種類(支店定義)
		String branchFileCategory = "支店定義";
		// ファイルの種類(商品定義)
		String commodityFileCategory = "商品定義";

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, branchRegEx, branchFileCategory)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, commodityRegEx, commodityFileCategory)) {
			return;
		}

		//listFilesを使用してfilesという配列に、
		//指定したパスに存在する全てのファイル(または、ディレクトリ)の情報を格納します。
		File[] files = new File(args[0]).listFiles();

		//先にファイルの情報を格納する List(ArrayList) を宣言します。
		List<File> rcdFiles = new ArrayList<>();

		//filesの数だけ繰り返すことで、
		//指定したパスに存在する全てのファイル(または、ディレクトリ)の数だけ繰り返されます。
		for(int i = 0; i < files.length ; i++) {
			if(files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				//対象がファイルであり、「数字8桁.rcd」なのか判定します。
				//売上ファイルの条件に当てはまったものだけ、List(ArrayList) に追加します。
				rcdFiles.add(files[i]); 
			}
		}

		//売上ファイルの連番チェック
		//比較回数は売上ファイルの数よりも1回少ないため、 
		//繰り返し回数は売上ファイルのリストの数よりも1つ小さい数です。
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() - 1; i++) {

			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

		    //比較する2つのファイル名の先頭から数字の8文字を切り出し、int型に変換します。
			if((latter - former) != 1) {
				//2つのファイル名の数字を比較して、差が1ではなかったら、
				//エラーメッセージをコンソールに表示します。
				System.out.println(FILE_NOT_EXIST_RCD);
				return;
			}
		}

		//rcdFilesに複数の売上ファイルの情報を格納しているので、その数だけ繰り返します。
		for(int i = 0; i < rcdFiles.size(); i++) {

			//支店(商品)定義ファイル読み込み(readFileメソッド)を参考に売上ファイルの中身を読み込みます。
			BufferedReader br = null;

			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				List<String> saleItems = new ArrayList<String>();
				String line;
				// 一行ずつ読み込む
				//売上ファイルの1行目には支店コード、2行目には商品コード、3行目には売上金額が入っています。
				while((line = br.readLine()) != null) {
					saleItems.add(line);
				}

				String rcdFileName = rcdFiles.get(i).getName();

				//売上ファイルのフォーマットチェック
				if(saleItems.size() != 3) {
				    //売上ファイルの行数が3行ではなかった場合は、
				    //エラーメッセージをコンソールに表示します。
					System.out.println(rcdFileName + FILE_INVALID_FORMAT_RCD);
					return;
				}
				//支店コードの存在チェック
				if (!branchNames.containsKey(saleItems.get(0))) {
					//支店情報を保持しているMapに売上ファイルの支店コードが存在しなかった場合は、
					//エラーメッセージをコンソールに表示します。
					System.out.println(rcdFileName + VALUE_NOT_EXIST_BRANCH_CODE);
					return;
				}
				//商品コードの存在チェック
				if (!commodityNames.containsKey(saleItems.get(1))) {
					//商品情報を保持しているMapに商品ファイルの商品コードが存在しなかった場合は、
					//エラーメッセージをコンソールに表示します。
					System.out.println(rcdFileName + VALUE_NOT_EXIST_COMMODITY_CODE);
					return;
				}
				//売上金額が数字なのかチェック
				if(!saleItems.get(2).matches("^[0-9]*$")) {
				    //売上金額が数字ではなかった場合は、
				    //エラーメッセージをコンソールに表示します。
					System.out.println(UNKNOWN_ERROR);
					return;
				}
				//売上ファイルから読み込んだ売上金額をMapに加算していくために、型の変換を行います。
				long fileSale = Long.parseLong(saleItems.get(2));

				//読み込んだ売上⾦額を加算します。
				Long branchSaleAmount = branchSales.get(saleItems.get(0)) + fileSale;
				Long commoditySaleAmount = commoditySales.get(saleItems.get(1)) + fileSale;
				//売上金額合計の桁数チェック
				if(branchSaleAmount >= 10000000000L || commoditySaleAmount >= 10000000000L){
					//売上金額が11桁以上の場合、エラーメッセージをコンソールに表示します。
					System.out.println(VALUE_INVALID_FORMAT_SALEAMOUNT);
					return;
				}

				//加算した売上⾦額をMapに追加します。
				branchSales.put(saleItems.get(0), branchSaleAmount);
				commoditySales.put(saleItems.get(1), commoditySaleAmount);
			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店(商品)定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店(商品)コードと支店(商品)名を保持するMap
	 * @param 支店(商品)コードと売上金額を保持するMap
	 * @param 支店(商品)コードの正規表現
	 * @param ファイルの種類
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales, String regEx, String fileCategory) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			//支店(商品)定義ファイルの存在チェック
			if(!file.exists()) {
				//支店(商品)定義ファイルが存在しない場合、
				//エラーメッセージをコンソールに表示します。
				System.out.println(fileCategory + FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				String[] items = line.split(",");
				//支店(商品)定義ファイルのフォーマットチェック
				if((items.length != 2) || (!items[0].matches(regEx))){
					//支店(商品)定義ファイルの仕様が満たされていない場合、
					//エラーメッセージをコンソールに表示します。
					System.out.println(fileCategory + FILE_INVALID_FORMAT);
					return false;
				}
				
				//Mapに追加する2つの情報を putの引数として指定します。
				names.put(items[0], items[1]);
				sales.put(items[0], 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店(商品)別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店(商品)コードと支店(商品)名を保持するMap
	 * @param 支店(商品)コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales) {
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			for (String key : names.keySet()) {
				//keyという変数には、Mapから取得したキーが代入されています。
				//拡張for文で繰り返されているので、1つ目のキーが取得できたら、
				//2つ目の取得...といったように、次々とkeyという変数に上書きされていきます。
				bw.write(key + "," + names.get(key) +  "," + sales.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}

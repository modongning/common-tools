package com.x.utils.excel;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.x.utils.codec.EncodeUtils;
import com.x.utils.collect.ListUtils;
import com.x.utils.excel.annotation.ExcelField;
import com.x.utils.excel.annotation.ExcelField.Align;
import com.x.utils.excel.annotation.ExcelField.Type;
import com.x.utils.excel.annotation.ExcelFields;
import com.x.utils.lang.ObjectUtils;
import com.x.utils.lang.StringUtils;
import com.x.utils.reflect.ReflectUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 导出Excel文件（导出“XLSX”格式，支持大数据量导出   @see org.apache.poi.ss.SpreadsheetVersion）
 */
public class ExcelExport {
	
	private static Logger log = LoggerFactory.getLogger(ExcelExport.class);

    /* 列高 */
    private static short rowHeight = 450;
			
	/**
	 * 工作薄对象
	 */
	private Workbook wb;
	
	/**
	 * 工作表对象
	 */
	private Sheet sheet;
	
	/**
	 * 样式列表
	 */
	private Map<String, CellStyle> styles;
	
	/**
	 * 当前行号
	 */
	private int rownum;
	
	/**
	 * 注解列表（Object[]{ ExcelField, Field/Method }）
	 */
	List<Object[]> annotationList;
	
	/**
	 * 构造函数
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 */
	public ExcelExport(String title, Class<?> cls){
		this(title, cls, Type.EXPORT);
	}

	/**
	 * 构造函数
	 * @param title 表格标题，传“空值”，表示无标题
     * @param headerList 表头数组
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 */
	public ExcelExport(String title, List<String> headerList, Class<?> cls){
	    this(title, cls, Type.EXPORT, headerList);
	}

	/**
	 * 构造函数
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public ExcelExport(String title, Class<?> cls, Type type, String... groups){
		this(null, null, title, cls, type, groups);
	}
	
	/**
	 * 构造函数
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public ExcelExport(String title, Class<?> cls, Type type, List<String> headerList, String... groups){
		this(null, null, title, cls, type, headerList, groups);
	}

	/**
	 * 构造函数
	 * @param sheetName 指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public ExcelExport(String sheetName, String title, Class<?> cls, Type type, String... groups){
		this(null, sheetName, title, cls, type, groups);
	}
	
	/**
	 * 构造函数
	 * @param wb 指定现有工作簿对象
	 * @param sheetName 指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public ExcelExport(Workbook wb, String sheetName, String title, Class<?> cls, Type type, String... groups){
		if (wb != null){
			this.wb = wb;
		}else{
			this.wb = createWorkbook();
		}
		this.createSheet(sheetName, title, cls, type, groups);
	}

	/**
	 * 构造函数
	 * @param wb 指定现有工作簿对象
	 * @param sheetName 指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public ExcelExport(Workbook wb, String sheetName, String title, Class<?> cls, Type type, List<String> headerList, String... groups){
		if (wb != null){
			this.wb = wb;
		}else{
			this.wb = createWorkbook();
		}
		this.createSheet(sheetName, title, cls, type, headerList, groups);
	}

	/**
	 * 构造函数
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param headerList 表头数组
	 */
	public ExcelExport(String title, List<String> headerList, List<Integer> headerWidthList) {
		this(null, null, title, headerList, headerWidthList);
	}
	
	/**
	 * 构造函数
	 * @param sheetName 指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param headerList 表头数组
	 */
	public ExcelExport(String sheetName, String title, List<String> headerList, List<Integer> headerWidthList) {
		this(null, sheetName, title, headerList, headerWidthList);
	}
	
	/**
	 * 构造函数
	 * @param wb 指定现有工作簿对象
	 * @param sheetName，指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param headerList 表头列表
	 */
	public ExcelExport(Workbook wb, String sheetName, String title, List<String> headerList, List<Integer> headerWidthList) {
		if (wb != null){
			this.wb = wb;
		}else{
			this.wb = createWorkbook();
		}
		this.createSheet(sheetName, title, headerList, headerWidthList);
	}
	
	/**
	 * 创建一个工作簿
	 */
	public Workbook createWorkbook(){
		return new SXSSFWorkbook(500);
	}

	/**
	 * 获取当前工作薄
	 * @author ThinkGem
	 */
	public Workbook getWorkbook() {
		return wb;
	}
	
	/**
	 * 创建工作表
	 * @param sheetName，指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public void createSheet(String sheetName, String title, Class<?> cls, Type type, String... groups){
		this.annotationList = ListUtils.newArrayList();
		// Get annotation field

        Field[] fs = cls.getDeclaredFields();
        //获取一级父类属性
        Field[] pfs = cls.getSuperclass().getName().equals("java.lang.Object")==true?
                new Field[0]:new Field[cls.getSuperclass().getDeclaredFields().length];
        //总的属性
        Field[] allFs = cls.getSuperclass().getName().equals("java.lang.Object")==true?
                new Field[fs.length]:new Field[cls.getSuperclass().getDeclaredFields().length+fs.length];
		if(!cls.getSuperclass().getName().equals("java.lang.Object")){
            pfs = cls.getSuperclass().getDeclaredFields();
        }
        allFs = ArrayUtils.addAll(fs, pfs);
        for (Field f : allFs){
			ExcelFields efs = f.getAnnotation(ExcelFields.class);
			if (efs != null && efs.value() != null){
				for (ExcelField ef : efs.value()){
					addAnnotation(annotationList, ef, f, type, groups);
				}
			}
			ExcelField ef = f.getAnnotation(ExcelField.class);
			addAnnotation(annotationList, ef, f, type, groups);
		}
		// Get annotation method
		Method[] ms = cls.getDeclaredMethods();
		for (Method m : ms){
			ExcelFields efs = m.getAnnotation(ExcelFields.class);
			if (efs != null && efs.value() != null){
				for (ExcelField ef : efs.value()){
					addAnnotation(annotationList, ef, m, type, groups);
				}
			}
			ExcelField ef = m.getAnnotation(ExcelField.class);
			addAnnotation(annotationList, ef, m, type, groups);
		}
		// Field sorting
		Collections.sort(annotationList, new Comparator<Object[]>() {
			@Override
			public int compare(Object[] o1, Object[] o2) {
				return new Integer(((ExcelField)o1[0]).sort()).compareTo(
						new Integer(((ExcelField)o2[0]).sort()));
			};
		});
		// Initialize
		List<String> headerList = ListUtils.newArrayList();
		List<Integer> headerWidthList = ListUtils.newArrayList();
		for (Object[] os : annotationList){
			ExcelField ef = (ExcelField)os[0];
			String headerTitle = ef.title();
			// 如果是导出，则去掉注释
			if (type == Type.EXPORT){
				String[] ss = StringUtils.split(headerTitle, "**", 2);
				if (ss.length == 2){
					headerTitle = ss[0];
				}
			}
			headerList.add(headerTitle);
			headerWidthList.add(ef.width());
		}
		// 创建工作表
		this.createSheet(sheetName, title, headerList, headerWidthList);
	}

	/**
	 * 创建工作表
	 * @param sheetName，指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param cls 实体对象，通过annotation.ExportField获取标题
	 * @param type 导出类型（1:导出数据；2：导出模板）
	 * @param groups 导入分组
	 */
	public void createSheet(String sheetName, String title, Class<?> cls, Type type, List<String> headerList, String... groups){
		this.annotationList = ListUtils.newArrayList();
		// Get annotation field

        Field[] fs = cls.getDeclaredFields();
        //获取一级父类属性
        Field[] pfs = cls.getSuperclass().getName().equals("java.lang.Object")==true?
                new Field[0]:new Field[cls.getSuperclass().getDeclaredFields().length];
        //总的属性
        Field[] allFs = cls.getSuperclass().getName().equals("java.lang.Object")==true?
                new Field[fs.length]:new Field[cls.getSuperclass().getDeclaredFields().length+fs.length];
		if(!cls.getSuperclass().getName().equals("java.lang.Object")){
            pfs = cls.getSuperclass().getDeclaredFields();
        }
        if(pfs.length!=0){
            allFs = ArrayUtils.addAll(fs, pfs);
        }
        for (Field f : allFs){
			ExcelFields efs = f.getAnnotation(ExcelFields.class);
			if (efs != null && efs.value() != null){
				for (ExcelField ef : efs.value()){
					addAnnotation(annotationList, ef, f, type, groups);
				}
			}
			ExcelField ef = f.getAnnotation(ExcelField.class);
			addAnnotation(annotationList, ef, f, type, groups);
		}
		// Get annotation method
		Method[] ms = cls.getDeclaredMethods();
		for (Method m : ms){
			ExcelFields efs = m.getAnnotation(ExcelFields.class);
			if (efs != null && efs.value() != null){
				for (ExcelField ef : efs.value()){
					addAnnotation(annotationList, ef, m, type, groups);
				}
			}
			ExcelField ef = m.getAnnotation(ExcelField.class);
			addAnnotation(annotationList, ef, m, type, groups);
		}
		// Field sorting
		Collections.sort(annotationList, new Comparator<Object[]>() {
			@Override
			public int compare(Object[] o1, Object[] o2) {
				return new Integer(((ExcelField)o1[0]).sort()).compareTo(
						new Integer(((ExcelField)o2[0]).sort()));
			};
		});
		// Initialize
		List<Integer> headerWidthList = ListUtils.newArrayList();
		for (Object[] os : annotationList){
			ExcelField ef = (ExcelField)os[0];
			headerWidthList.add(ef.width());
		}
		// 创建工作表
		this.createSheet(sheetName, title, headerList, headerWidthList);
	}

	/**
	 * 添加到 annotationList
	 */
	private void addAnnotation(List<Object[]> annotationList, ExcelField ef, Object fOrM, Type type, String... groups){
//		if (ef != null && (ef.type()==0 || ef.type()==type)){
		if (ef != null && (ef.type() == Type.ALL || ef.type() == type)){
			if (groups != null && groups.length > 0){
				boolean inGroup = false;
				for (String g : groups){
					if (inGroup){
						break;
					}
					for (String efg : ef.groups()){
						if (StringUtils.equals(g, efg)){
							inGroup = true;
							annotationList.add(new Object[]{ef, fOrM});
							break;
						}
					}
				}
			}else{
				annotationList.add(new Object[]{ef, fOrM});
			}
		}
	}
	
	/**
	 * 创建工作表
	 * @param sheetName 指定Sheet名称
	 * @param title 表格标题，传“空值”，表示无标题
	 * @param headerList 表头字段设置
	 * @param headerWidthList 表头字段宽度设置
	 */
	public void createSheet(String sheetName, String title, List<String> headerList, List<Integer> headerWidthList) {
		this.sheet = wb.createSheet(StringUtils.defaultString(sheetName, "Sheet1"));
		this.styles = createCellStyle(wb);
		this.rownum = 0;
		// Create title
		if (StringUtils.isNotBlank(title)){
			Row titleRow = sheet.createRow(rownum++);
			titleRow.setHeightInPoints(30);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellStyle(styles.get("title"));
			titleCell.setCellValue(title);
			sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(),
					titleRow.getRowNum(), titleRow.getRowNum(), headerList.size()-1));
		}
		// Create header
		if (headerList == null){
			throw new ExcelException("headerList not null!");
		}
		Row headerRow = sheet.createRow(rownum++);
        headerRow.setHeight(rowHeight);// 高度
		headerRow.setHeightInPoints(16);
		for (int i = 0; i < headerList.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(styles.get("header"));
			String[] ss = StringUtils.split(headerList.get(i), "**", 2);
			if (ss.length==2){
				cell.setCellValue(ss[0]);
				Comment comment = this.sheet.createDrawingPatriarch().createCellComment(
						new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 5, 6));
				comment.setRow(cell.getRowIndex());
				comment.setColumn(cell.getColumnIndex());
				comment.setString(new XSSFRichTextString(ss[1]));
				cell.setCellComment(comment);
			}else{
				cell.setCellValue(headerList.get(i));
			}
//			sheet.autoSizeColumn(i);
		}
		boolean isDefWidth = (headerWidthList != null && headerWidthList.size() == headerList.size());
		for (int i = 0; i < headerList.size(); i++) {
			int colWidth = -1;
			if (isDefWidth){
				colWidth = headerWidthList.get(i);
			}
			if (colWidth == -1){
				colWidth = sheet.getColumnWidth(i)*2;
				colWidth = colWidth < 3000 ? 3000 : colWidth;
			}
			if (colWidth == 0){
				sheet.setColumnHidden(i, true);
			}else{
				sheet.setColumnWidth(i, colWidth);  
			}
		}
		log.debug("Create sheet {} success.", sheetName);
	}

	/**
	 * 文本样式
	 *
	 * @param workbook
	 *            文字大小
	 */
	public Map<String, CellStyle> createCellStyle(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        styles.put("title",  null);
        styles.put("data",   null);
        styles.put("data1",  null);
        styles.put("data2",  null);
        styles.put("data3",  null);
        styles.put("header", null);
        styles.put("none", null);
        for (Map.Entry<String, CellStyle> entry : styles.entrySet()) {
            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setBorderLeft(CellStyle.BORDER_THIN); // 左边边框
            bodyStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex()); // 左边边框颜色
            bodyStyle.setBorderRight(CellStyle.BORDER_THIN); // 右边边框
            bodyStyle.setRightBorderColor(IndexedColors.BLACK.getIndex()); // 右边边框颜色
            bodyStyle.setBorderBottom(CellStyle.BORDER_THIN); // 下边框
            bodyStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex()); // 下边框颜色
            bodyStyle.setAlignment(CellStyle.ALIGN_CENTER);// 设置单元格水平方向对其方式
            bodyStyle.setVerticalAlignment(CellStyle.ALIGN_LEFT); // 设置单元格垂直方向对其方式
            if(entry.getKey().equals("header")){
                // XLS标题头样式 header style
                Font headFont = workbook.createFont();
                headFont.setFontHeightInPoints((short) 12);// 大小
                headFont.setBoldweight(Font.BOLDWEIGHT_BOLD);// 粗体显示
                headFont.setFontName("黑体");
                bodyStyle.setFont(headFont);
                bodyStyle.setBorderTop(CellStyle.BORDER_THIN);
                bodyStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                bodyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                bodyStyle.setFillPattern(CellStyle.SOLID_FOREGROUND); // 前景色
            } else if(entry.getKey().equals("none")){
                bodyStyle = workbook.createCellStyle();
            } else {
                bodyStyle.setWrapText(true);// 自动换行
            }
            styles.put(entry.getKey(), bodyStyle);
        }
		return styles;
	}

	/**
	 * 创建表格样式
	 * @param wb 工作薄对象
	 * @return 样式列表
	 */
	private Map<String, CellStyle> createStyles(Workbook wb) {
		Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
		
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		Font titleFont = wb.createFont();
		titleFont.setFontName("Arial");
		titleFont.setFontHeightInPoints((short) 16);
		titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		style.setFont(titleFont);
		styles.put("title", style);

		style = wb.createCellStyle();
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		Font dataFont = wb.createFont();
		dataFont.setFontName("Arial");
		dataFont.setFontHeightInPoints((short) 10);
		style.setFont(dataFont);
		styles.put("data", style);
		
		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_LEFT);
		styles.put("data1", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_CENTER);
		styles.put("data2", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		styles.put("data3", style);
		
		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
//		style.setWrapText(true);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		Font headerFont = wb.createFont();
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 10);
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(headerFont);
		styles.put("header", style);
		
		return styles;
	}

	/**
	 * 添加一行
	 * @return 行对象
	 */
	public Row addRow(){
		return sheet.createRow(rownum++);
	}

	/**
	 * 添加一个单元格
	 * @param row 添加的行
	 * @param column 添加列号
	 * @param val 添加值
	 * @return 单元格对象
	 */
	public Cell addCell(Row row, int column, Object val){
		return this.addCell(row, column, val, Align.AUTO, Class.class, null);
	}
	
	/**
	 * 添加一个单元格
	 * @param row 添加的行
	 * @param column 添加列号
	 * @param val 添加值
	 * @param align 对齐方式（1：靠左；2：居中；3：靠右）
	 * @param dataFormat 数值格式（例如：0.00，yyyy-MM-dd）
	 * @return 单元格对象
	 */
	public Cell addCell(Row row, int column, Object val, Align align, Class<?> fieldType, String dataFormat){
		Cell cell = row.createCell(column);
		String defaultDataFormat = "@";
		try {
			if(val == null){
				cell.setCellValue("");
			}else if(fieldType != Class.class){
				cell.setCellValue((String)fieldType.getMethod("setValue", Object.class).invoke(null, val));
				try{
					defaultDataFormat = (String)fieldType.getMethod("getDataFormat").invoke(null);
				} catch (Exception ex) {
					defaultDataFormat = "@";
				}
			}else{
				if(val instanceof String) {
					cell.setCellValue((String) val);
				}else if(val instanceof Integer) {
					cell.setCellValue((Integer) val);
					defaultDataFormat = "0";
				}else if(val instanceof Long) {
					cell.setCellValue((Long) val);
					defaultDataFormat = "0";
				}else if(val instanceof Double) {
					cell.setCellValue((Double) val);
					defaultDataFormat = "0.00";
				}else if(val instanceof Float) {
					cell.setCellValue((Float) val);
					defaultDataFormat = "0.00";
				}else if(val instanceof Date) {
					cell.setCellValue((Date) val);
					defaultDataFormat = "yyyy-MM-dd HH:mm";
				}else {
					cell.setCellValue((String)Class.forName(this.getClass().getName().replaceAll(this.getClass().getSimpleName(), 
						"fieldtype."+val.getClass().getSimpleName()+"Type")).getMethod("setValue", Object.class).invoke(null, val));
				}
			}
//			if (val != null){
				CellStyle style = styles.get("data_column_"+column);
				if (style == null){
					style = wb.createCellStyle();
					style.cloneStyleFrom(styles.get("data"+(align.value()>=1&&align.value()<=3?align.value():"")));
					if (dataFormat != null){
						defaultDataFormat = dataFormat;
					}
			        style.setDataFormat(wb.createDataFormat().getFormat(defaultDataFormat));
					styles.put("data_column_" + column, style);
				}
				cell.setCellStyle(style);
//			}
		} catch (Exception ex) {
			log.info("Set cell value ["+row.getRowNum()+","+column+"] error: " + ex.toString());
			cell.setCellValue(ObjectUtils.toString(val));
		}
		return cell;
	}

	//添加表头
	public void AddHeader(Map<String, String> headerMap){
        Row row = this.addRow();
        row.setHeight(rowHeight);// 高度
        row.setHeightInPoints(30);
        int colunm = 0;
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            Cell titleCell = row.createCell(colunm++);
            titleCell.setCellStyle(styles.get("header"));
            titleCell.setCellValue(entry.getValue());
        }
    }

	//根据表头取表格数据
    public <E> ExcelExport setDataList(List<E> list, Map<String, String> headerMap, Map<String, Double> sumFlagList){
        for (E e : list){
            int colunm = 0;
            Row row = this.addRow();
            row.setHeight(rowHeight);// 高度
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                //标识字段是否存在对应值
                Boolean existFlag = false;
                for (int i = 0; i < annotationList.size(); i++) {
                    Object[] os = annotationList.get(i);
                    ExcelField ef = (ExcelField)os[0];
                    if(!ef.attrName().equals(entry.getKey())){
                        continue;
                    }
                    existFlag = true;
                    Object val = null;
                    // Get entity value
                    try{
                        if (StringUtils.isNotBlank(ef.attrName())){
                            val = ReflectUtils.invokeGetter(e, ef.attrName());
                        }else{
                            if (os[1] instanceof Field){
                                val = ReflectUtils.invokeGetter(e, ((Field)os[1]).getName());
                            }else if (os[1] instanceof Method){
                                val = ReflectUtils.invokeMethod(e, ((Method)os[1]).getName(), new Class[] {}, new Object[] {});
                            }
                        }
                        // If is dict, get dict label
                        if (StringUtils.isNotBlank(ef.dictType())){
                            Class<?> dictUtils = Class.forName("com.jeesite.modules.sys.utils.DictUtils");
                            val = dictUtils.getMethod("getDictLabel", String.class, String.class,
                                    String.class).invoke(null, ef.dictType(), val==null?"":val.toString(), "");
                            //val = DictUtils.getDictLabel(val==null?"":val.toString(), ef.dictType(), "");
                        }
                    }catch(Exception ex) {
                        // Failure to ignore
                        log.info(ex.toString());
                        val = "";
                    }
                    String dataFormat = ef.dataFormat();
                    try {
                        // 获取Json格式化注解的格式化参数
                        JsonFormat jf = e.getClass().getMethod("get"+StringUtils.capitalize(ef.attrName())).getAnnotation(JsonFormat.class);
                        if (jf != null && jf.pattern() != null){
                            dataFormat = jf.pattern();
                        }
                    } catch (Exception e1) {
                        // 如果获取失败，则使用默认。
                    }
                    this.addCell(row, colunm++, val, ef.align(), ef.fieldType(), dataFormat);
                }
                //如果不存在,则插入一个空格
                if(!existFlag){
                    this.addCell(row, colunm++, null, Align.AUTO, String.class, "@");
                }
            }
        }
        addTotalRow(headerMap, sumFlagList);
        return this;
    }

    public void addTotalRow(Map<String, String> headerMap, Map<String, Double> sumFlagList){
        Row row = this.addRow();
        row.setHeight(rowHeight);// 高度
        row.setHeightInPoints(25);
        int colunm = 0;
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            boolean flag = false;
            Cell titleCell = row.createCell(colunm++);
            if(null != sumFlagList){
	            for(Map.Entry<String, Double> entry1 : sumFlagList.entrySet()){
		            if(entry1.getKey().equals(entry.getKey())) {
			            flag = true;
			            titleCell.setCellStyle(styles.get("header"));
			            titleCell.setCellValue(entry1.getValue());
		            }
	            }
            }
            if(!flag){
                titleCell.setCellStyle(styles.get("none"));
                titleCell.setCellValue("");
            }
        }
    }

	/**
	 * 添加数据（通过annotation.ExportField添加数据）
	 * @return list 数据列表
	 */
	public <E> ExcelExport setDataList(List<E> list){
		for (E e : list){
			int colunm = 0;
			Row row = this.addRow();
			StringBuilder sb = new StringBuilder();
			for (Object[] os : annotationList){
				ExcelField ef = (ExcelField)os[0];
				Object val = null;
				// Get entity value
				try{
					if (StringUtils.isNotBlank(ef.attrName())){
						val = ReflectUtils.invokeGetter(e, ef.attrName());
					}else{
						if (os[1] instanceof Field){
							val = ReflectUtils.invokeGetter(e, ((Field)os[1]).getName());
						}else if (os[1] instanceof Method){
							val = ReflectUtils.invokeMethod(e, ((Method)os[1]).getName(), new Class[] {}, new Object[] {});
						}
					}
					if (StringUtils.isNotBlank(ef.dictType())){
						Class<?> dictUtils = Class.forName("com.jeesite.modules.sys.utils.DictUtils");
						val = dictUtils.getMethod("getDictLabel", String.class, String.class,
									String.class).invoke(null, ef.dictType(), val==null?"":val.toString(), "");
					}
				}catch(Exception ex) {
					log.error(ex.toString());
					val = "";
				}
				String dataFormat = ef.dataFormat();
				try {
					// 获取Json格式化注解的格式化参数
					JsonFormat jf = e.getClass().getMethod("get"+StringUtils.capitalize(ef.attrName())).getAnnotation(JsonFormat.class);
					if (jf != null && jf.pattern() != null){
						dataFormat = jf.pattern();
					}
				} catch (Exception e1) {
					// 如果获取失败，则使用默认。
				}
				this.addCell(row, colunm++, val, ef.align(), ef.fieldType(), dataFormat);
				sb.append(val + ", ");
			}
			log.debug("Write success: ["+row.getRowNum()+"] "+sb.toString());
		}
		return this;
	}
	
	/**
	 * 输出数据流
	 * @param os 输出数据流
	 */
	public ExcelExport write(OutputStream os){
		try{
			wb.write(os);
		}catch(IOException ex){
			log.error(ex.getMessage(), ex);
		}
		return this;
	}
	
	/**
	 * 输出到客户端
	 * @param fileName 输出文件名
	 */
	public ExcelExport write(HttpServletResponse response, String fileName){
		response.reset();
        response.setContentType("application/octet-stream; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename="+EncodeUtils.encodeUrl(fileName));
		try {
			write(response.getOutputStream());
		} catch (IOException ex) {
			log.error(ex.getMessage(), ex);
		}
		return this;
	}
	
	/**
	 * 输出到文件
	 * @param  name 输出文件名
	 */
	public ExcelExport writeFile(String name) throws FileNotFoundException, IOException{
		FileOutputStream os = new FileOutputStream(name);
		this.write(os);
		return this;
	}
	
	/**
	 * 清理临时文件
	 */
	public ExcelExport dispose(){
		if (wb instanceof SXSSFWorkbook){
			((SXSSFWorkbook)wb).dispose();
		}
		return this;
	}
	
//	/**
//	 * 导出测试
//	 */
//	public static void main(String[] args) throws Throwable {
//		
//		// 初始化表头
//		List<String> headerList = ListUtils.newArrayList();
//		for (int i = 1; i <= 10; i++) {
//			headerList.add("表头"+i);
//		}
//
//		// 初始化数据集
//		List<String> rowList = ListUtils.newArrayList();
//		for (int i = 1; i <= headerList.size(); i++) {
//			rowList.add("数据"+i);
//		}
//		List<List<String>> dataList = ListUtils.newArrayList();
//		for (int i = 1; i <=100; i++) {
//			dataList.add(rowList);
//		}
//		
//		// 创建一个Sheet表，并导入数据
//		ExcelExport ee = new ExcelExport("表格1", "表格标题1", headerList, null);
//		for (int i = 0; i < dataList.size(); i++) {
//			Row row = ee.addRow();
//			for (int j = 0; j < dataList.get(i).size(); j++) {
//				ee.addCell(row, j, dataList.get(i).get(j));
//			}
//		}
//		
//		// 再创建一个Sheet表，并导入数据
//		ee.createSheet("表格2", "表格标题2", headerList, null);
//		for (int i = 0; i < dataList.size(); i++) {
//			Row row = ee.addRow();
//			for (int j = 0; j < dataList.get(i).size(); j++) {
//				ee.addCell(row, j, dataList.get(i).get(j)+"2");
//			}
//		}
//		
//		// 输出到文件
//		ee.writeFile("target/export.xlsx");
//
//		// 清理销毁
//		ee.dispose();
//		
//		log.debug("Export success.");
//		
//	}

}

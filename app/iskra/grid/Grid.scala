package iskra.grid

import play.api.mvc.Call
import play.twirl.api.Html

/**
 * Устанавливает типы полей для запроса.
 * columnsDatatypes - содержит строку букв разделенных символом "|" каждая буква обозначает тип данных для колонки результата запроса
 *                                S - все строковые типы данных, D - дата/время, N - числовые типы данных, B - Булев
 */
case class Grid(titles: Array[String],
					 values: Iterable[Iterable[Any]],
					 hiddenColumns: Array[String] = Array(),
					 hiddenInputs: Map[String, String] = Map.empty,
					 clicks: Map[String, String] = Map(),
					 idColumnName: String = "id",
					 generalColumnName: Option[String] = None,
					 labelColumnName: String = "",
					 formURL: Option[Call] = None,
					 isNumeration: Boolean = false,
					 errors: Seq[String] = Nil,
					 buttons: Seq[RowButton] = Seq(),
					 styles: Map[String, Map[String, String]] = Map(),
					 actions: Seq[(String, String)] = Nil,
					 doSelectAll: Boolean = false,
					 columnsDatatypes: Option[String] = None,
					 sortAscColumn: Option[String] = None,
					 sortDescColumn: Option[String] = None
	) {

//// устанавливаем видимые строки
//		for($i = 0; $i < $this->_cols; $i++)
//		{
//		    // если столбец скрыт, то переходим к следующему
//		    if( count($this->_hiden_columns) > 0  && in_array(mysql_field_name($this->_recordset, $i), $this->_hiden_columns) ) continue
//// запоминаем название столбца и считаем количество видимых колонок
//		    $this->_columnNames[ $this->_visibleColumnsCount++ ] = mysql_field_name($this->_recordset, $i)
//}
//
//
//	    if(!$this->_is_checkboxes && count($this->_actions) > 0 )
//	    	$this->_is_checkboxes = true
//// проверим количество элементов массива типов данных для столбцов соответствует реальному количеству столбцов или нет
//	    if( isset( $this->_columnsDatatypes ) && count($this->_columnsDatatypes) != $this->_visibleColumnsCount )
//	    {
//	      	echo Style::error( sprintf("Проверьте аргументы, передаваемые в Grid::setQueryColumnsDatatypes(): количество столбцов (%d) не соответствует реальному (%d)!) ",
//	      	                           count($this->_columnsDatatypes), $this->_visibleColumnsCount) )
//return false;
//	    }
//	}
//

	// TODO все ошибки через обработчик пропустить!!!!

	def html: Html = {
		import views.html.iskra.grid.table
		val indexedStyles = styles.map{case (title -> values) => titles.indexOf(title) -> values}
		val indexedClicks = clicks.map{case (title -> link) => titles.indexOf(title) -> link}
		val idIndex = titles.indexOf(idColumnName)
		//TODO add id to hidden columns
		table(titles,
			values,
			isNumeration,
			doSelectAll,
			actions.map{case (c,n) => c.toString -> n},
			hiddenInputs,
			buttons,
			indexedStyles,
			idIndex,
			indexedClicks,
			formURL
		)
	}





}

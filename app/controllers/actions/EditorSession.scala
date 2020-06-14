package controllers.actions

import play.twirl.api.Html

trait EditorSession {

  val base = Some(MenuItem(151, "TODO")) // , $_SESSION["base"]
  val section = Some(MenuItem(151, "TODO")) // $_SESSION["section"]
  val question = Some(MenuItem(151, "TODO")) // $_SESSION["question"]
  val template = Some(MenuItem(151, "TODO")) // $_SESSION['template_id']
  val currentGroupId = Some(MenuItem(151, "TODO")) // $_SESSION["current_group_id"]
  val user = Some(MenuItem(151, "TODO")) //  $_SESSION["user_id"]

  def oldEditNavigation: Html = {
    views.html.editor.oldEditNavigation(base, section, question)
  }

//  {
//    if( $_SESSION['group'] == Main::ADMIN_GROUP_NAME ) return 0
//    $sql = sprintf("SELECT read_only+0 FROM bases_access WHERE base_id = %d AND user_id = %d", $_SESSION['base_id'], $_SESSION['user_id'])
//    return SQL::getSQLValue( $sql )
//  }
}

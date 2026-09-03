package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class SwaggerController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  def index: Action[AnyContent] = Action {
    Ok(SwaggerController.indexHtml).as("text/html")
  }
}

object SwaggerController {

  private val indexHtml: String =
    """<!DOCTYPE html>
      |<html>
      |  <head>
      |    <title>Transaction Validator API</title>
      |    <link rel="stylesheet" href="/assets/lib/swagger-ui/swagger-ui.css" />
      |  </head>
      |  <body>
      |    <div id="swagger-ui"></div>
      |    <script src="/assets/lib/swagger-ui/swagger-ui-bundle.js"></script>
      |    <script>
      |      window.onload = () => {
      |        window.ui = SwaggerUIBundle({
      |          url: "/assets/openapi.yaml",
      |          dom_id: "#swagger-ui"
      |        });
      |      };
      |    </script>
      |  </body>
      |</html>
      |""".stripMargin
}

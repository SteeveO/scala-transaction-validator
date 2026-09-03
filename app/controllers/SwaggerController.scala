package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class SwaggerController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  private val WebjarBasePath = "/META-INF/resources/webjars/swagger-ui/5.17.14/"

  def index: Action[AnyContent] = Action {
    Ok(SwaggerController.indexHtml).as("text/html")
  }

  def asset(file: String): Action[AnyContent] = Action {
    Option(getClass.getResourceAsStream(WebjarBasePath + file)) match {
      case Some(stream) =>
        val bytes = stream.readAllBytes()
        stream.close()
        Ok(bytes).as(SwaggerController.contentTypeFor(file))
      case None =>
        NotFound
    }
  }
}

object SwaggerController {

  private def contentTypeFor(file: String): String =
    if (file.endsWith(".css")) "text/css"
    else if (file.endsWith(".js")) "application/javascript"
    else if (file.endsWith(".html")) "text/html"
    else if (file.endsWith(".map")) "application/json"
    else "application/octet-stream"

  private val indexHtml: String =
    """<!DOCTYPE html>
      |<html>
      |  <head>
      |    <title>Transaction Validator API</title>
      |    <link rel="stylesheet" href="/docs/swagger-ui/swagger-ui.css" />
      |  </head>
      |  <body>
      |    <div id="swagger-ui"></div>
      |    <script src="/docs/swagger-ui/swagger-ui-bundle.js"></script>
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

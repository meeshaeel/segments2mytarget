package net.facetz.mailru

import java.text.SimpleDateFormat
import java.util.Date

object Runner extends App {

  val today = new SimpleDateFormat("yyyyMMdd").format(new Date())

  case class Config(workingDirectory: String = "",
                    partnerId: String = "",
                    outputFolderName: String = "results",
                    dateStr: String = today,
                    regexp: String = ".*[.gz]",
                    upload: Boolean = false,
                    auditoryUpdate: Boolean = false,
                    apiUrl: String = "https://target.my.com",
                    clientId: String = "",
                    clientSecret: String = "",
                    subAccountName: Option[String] = None
                     )

  // args example: -i /tmp/gz -p dl -u -c someclient -s somesecret
  val parser = new scopt.OptionParser[Config]("mailru-segment-exporter") {
    head("mailru-segment-exporter")
    help("help") text "prints this usage text"
    opt[String]('i', "workdir")
      .valueName("<workdir>")
      .action({ (value, config) => config.copy(workingDirectory = value) })
      .text("Directory with files to process. ex. /opt/segments")
    opt[String]('p', "partner")
      .valueName("<partner>")
      .action({ (value, config) => config.copy(partnerId = value) })
      .text("mailru partner prefix, will be first line of each processed file")
    opt[String]('o', "outputname")
      .valueName("<outputname>")
      .action({ (value, config) => config.copy(outputFolderName = value) })
      .text("output folder name with parsing results. 'results' by default")
    opt[String]('d', "date")
      .valueName("<date>")
      .action({ (value, config) => config.copy(dateStr = value) })
      .text("suffix of segment file name. It will be used for auditory update in future. ex. 20151231, now by default")
    opt[String]('r', "regexp")
      .valueName("<regexp>")
      .action({ (value, config) => config.copy(regexp = value) })
      .text("source filename pattern in workdir, default .*(.gz)$")
    opt[Unit]('u', "upload")
      .valueName("<upload>")
      .action({ (_, config) => config.copy(upload = true) })
      .text("upload segments to mailru or not. false by default")
    opt[Unit]('y', "auditoryupdate")
      .valueName("<auditoryupdate>")
      .action({ (_, config) => config.copy(auditoryUpdate = true) })
      .text("update auditories in mailru or not. false by default")
    opt[Unit]('a', "apiurl")
      .valueName("<apiurl>")
      .action({ (_, config) => config.copy(upload = true) })
      .text("mailru api url, https://target.my.com by default")
    opt[String]('c', "client")
      .valueName("<client>")
      .action({ (value, config) => config.copy(clientId = value) })
      .text("your mailru client_id")
    opt[String]('s', "secret")
      .valueName("<secret>")
      .action({ (value, config) => config.copy(clientSecret = value) })
      .text("your mailru client_secret")
    opt[String]('m', "minion")
      .valueName("<minion>")
      .action({ (value, config) => config.copy(subAccountName = Some(value)) })
      .text("subaccount name for agencies")

    checkConfig(c =>
      if (c.upload && c.auditoryUpdate) {
        failure("only one mode accepted: upload or auditoryupdate")
      } else if (c.upload && (c.clientId.isEmpty || c.clientSecret.isEmpty)) {
        failure("you want to upload but not set clientId or clientSecret")
      } else if (!c.auditoryUpdate && (c.workingDirectory.isEmpty || c.partnerId.isEmpty)) {
        failure("you want process file but not set workingDirectory or partnerId")
      } else if (!c.upload && !c.auditoryUpdate && (!c.clientId.isEmpty || c.clientSecret.isEmpty)) {
        println("you set clientId or clientSecret but not set -u option, files will not be uploaded")
        success
      } else {
        success
      }
    )
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      ConfigHolder.config = config
      MailRuExport.run()
      sys.exit()
    case None =>
      sys.exit(1)
  }
}

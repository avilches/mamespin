<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${opts.filename}</title>
    <link rel="shortcut icon" href="https://filecloud.io/favicon.ico">
    <meta name="description" content="free file backup and storage in the cloud"/>
    <link href="//cdn.jsdelivr.net/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
    <link href="//cdn.jsdelivr.net/bootstrap/3.3.7/css/bootstrap-theme.min.css" rel="stylesheet">
    <link href="//cdn.jsdelivr.net/fontawesome/4.6.3/css/font-awesome.min.css" rel="stylesheet">
    <style type="text/css">html {
        position: relative;
        min-height: 100%;
    }

    body {
        margin-bottom: 30px;
        background-color: #f5f5f5;
    }

    #footer {
        position: absolute;
        bottom: 0;
        width: 100%;
        height: 30px;
        background-color: #f5f5f5;
        text-align: center;
        border-top: 1px solid white;
    }

    body > .container {
        padding: 60px 15px 0;
    }

    body > .container-fluid {
        padding: 60px 15px 0;
    }

    #ezLogoLink {
        color: #FF8400;
        font-size: 160%;
        font-weight: bold;
    }</style>
</head>
<body>

<div id="header" class="navbar navbar-default navbar-fixed-top navbar-inverse" role="navigation">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" id="ezLogoLink" href="https://filecloud.io/"><i class="fa fa-cloud fa-fw"></i>&nbsp;mamespin CDN</a>
        </div>
    </div>
</div>

<div class="container" id="_container">
    <br/>
    <div class="row" id="row2">
        <div class="col-md-8 col-md-push-2 text-center">
            <div style="margin: 0px auto; text-align:center" id="dlFld">
                <div class="panel panel-default">
                    <div class="panel-heading text-left">
                        <h3 style="margin-top:10px"><i
                                class="fa fa-download fa-fw"></i>&nbsp;${opts.filename} <span style="float:right">${opts.getFileSizeString()}</span></h3>
                    </div>
                    <div class="panel-body" id="recaptchaPanel">
                            <div class="text-left">
                                <p>Slots: ${opts.slots}</p>

                            </div>
                        </div>
                                <div class="panel-body" id="recaptchaPanel">
                                        <div class="text-right">
<p>Limite velocidad: ${opts.cpsMsg} </p>
<p>Tiempo estimado de descarga: ${opts.getETA()}</p>



                        </div>
                    </div>
                    <div class="panel-footer text-center" id="downloadFooterPanel"><a class="btn btn-primary btn-lg"
                                                                                      href="' + data.downloadUrl + '">Download
                        File</a></div>
                </div>
            </div>
        </div>
    </div>
</div>
<div id="footer">
    <div class="container">
        <p>
            <a rel="nofollow" href="https://filecloud.io/?m=apidoc"><i class="fa fa-cogs"></i> API</a>
            &nbsp;&nbsp;&nbsp;
            <a rel="nofollow" href="https://filecloud.io/?m=help&a=contact"><i class="fa fa-envelope-o"></i> Contact</a>
            &nbsp;&nbsp;&nbsp;
            <a rel="nofollow" href="https://filecloud.io/?m=help&a=copyright"><i class="fa fa-gavel"></i> Copyright
                Policy</a>
            &nbsp;&nbsp;&nbsp;
            <a rel="nofollow" href="https://filecloud.io/?m=help&a=privacy"><i class="fa fa-user-secret"></i> Privacy
                Policy</a>
            &nbsp;&nbsp;&nbsp;
            <a rel="nofollow" href="https://filecloud.io/?m=help&a=tos"><i class="fa fa-check-square-o"></i> Terms of
                Service</a>
        </p>
    </div>
</div>
<script src="//cdn.jsdelivr.net/jquery/3.1.0/jquery.min.js"></script>
<script src="//cdn.jsdelivr.net/bootstrap/3.3.7/js/bootstrap.min.js"></script>
</body>
</html>
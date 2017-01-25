<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <title>${opts.filename}</title>

    <link href="//cdn.jsdelivr.net/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">
    <link href="//cdnjs.cloudflare.com/ajax/libs/bootswatch/3.3.7/slate/bootstrap.min.css" rel="stylesheet">
    <link href="//cdn.jsdelivr.net/fontawesome/4.6.3/css/font-awesome.min.css" rel="stylesheet">
    <style type="text/css">
    body {
        /*background: url('/assets/background/background-night.jpg') no-repeat fixed 100% 100%;*/
        background-size: cover;
    }

    .info-slots {
        font-weight: 200;
        font-size: 20px;
        margin-right: 4px;
        color: cornsilk;
        background: #5c8e62;
        padding: 5px 11px;
        border-radius: 10px
    }
    </style>
</head>

<body>
<div class="container">
    <div class="row">
        <div class="col-lg-8 col-lg-push-2">
            <div class="panel panel-success" style="margin-top:100px">
                <div class="panel-heading" style="padding: 13px 0 12px 8px">
                    <h1 class="panel-title">Límite: <span class="info-slots">${opts.cpsMsg}</span> Slots: <span
                            class="info-slots">${opts.currentDownloads}/${opts.slots}</span></h1>
                </div>

                <div class="panel-body" style="text-align: center">
                    <h3 style="margin:0; font-weight: bold">${opts.filename}</h3>
                    <hr/>

                    <p>Tamaño archivo: <span
                            style="font-weight: bold">${opts.getFileSizeString()}</span><br/>Tiempo estimado de descarga: <span
                            style="font-weight: bold">${opts.getETA()}</span></p>

                    <p></p>

                    <a href="/download/${opts.token}" class="btn btn-primary"><i
                            class="fa fa-download fa-fw"></i> Descargar</a>

                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
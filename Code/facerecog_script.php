<?php

#function for streaming file to client
function streamFile($location, $filename, $mimeType='application/octet-stream', $execution_time)
{ if(!file_exists($location))
  { header ("HTTP/1.0 404 Not Found");
    return;
  }

  $size=filesize($location);
  $time=date('r',filemtime($location));
  #html response header
  header('Content-Description: File Transfer');
  header("Content-Type: $mimeType");
  header('Cache-Control: public, must-revalidate, max-age=0');
  header('Pragma: no-cache');
  header('Accept-Ranges: bytes');
  header('Content-Length:'.($size));
  header("Content-Disposition: inline; filename=$filename");
  header("Content-Transfer-Encoding: binary\n");
  header("Last-Modified: $time");
  header('Servername: hpvm');
  header('Process_time:'.($execution_time));
  header('Connection: close');

  ob_clean();
  flush();
  readfile($location);

}

#**********************************************************
#Main script
#**********************************************************

#<1>set target path for storing photo uploads on the server
$photo_upload_path = "./upload/";
$photo_upload_path = $photo_upload_path. basename( $_FILES['uploadedfile']['name']);

#<2>set target path for storing result on the server
$processed_photo_output_path = "./output/processed_";
$processed_photo_output_path = $processed_photo_output_path. basename( $_FILES['uploadedfile']['name']);
$downloadFileName = 'processed_' . basename( $_FILES['uploadedfile']['name']);

#<3>modify maximum allowable file size to 10MB and timeout to 300s
ini_set('upload_max_filesize', '10M');
ini_set('post_max_size', '10M');
ini_set('max_input_time', 300);
ini_set('max_execution_time', 300);

#<4>Get and stored uploaded photos on the server
if(copy($_FILES['uploadedfile']['tmp_name'], $photo_upload_path)) {

        #<5> execute opencv(c++) code for face recognition
        $command = "./facerecog '$photo_upload_path' '$processed_photo_output_path'";
		
	   #<6> profiling time to process the image and getting face recognition results
        $time_start = microtime(true);
        exec($command);
        $time_end = microtime(true);

        $execution_time = ($time_end - $time_start);

        #<6>stream processed photo to the client
        streamFile($processed_photo_output_path, $downloadFileName,"application/octet-stream", $execution_time);

} else{
    echo "There was an error uploading the file to $photo_upload_path !";
}

?>

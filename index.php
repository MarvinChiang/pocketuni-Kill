<?php
//请求示例：https://github.com/dysf888/pocketuni-Kill/edit/master/index.php?time=yyyyMMdd HHmmss&id=活动id
$outurl = urldecode( 'http://'.$_SERVER['HTTP_HOST'].$_SERVER['PHP_SELF'].'?'.$_SERVER['QUERY_STRING']);
$arr = parse_url($outurl);
$arr_query = convertUrlQuery($arr['query']);
$arr_query= json_encode($arr_query);
$config = json_decode($arr_query);
$time = $config -> time;
$id = $config -> id ;
if ($id != '' && $time != '' ){
    $set = 'remarkName=TEST
activityID_1='.$id.'
mySchedule='.$time.'
ifSchedule=true
Student_ID=0490201XX
Password=xxxxxxxxx
activityID_2=0
ThreadPool_Size=5
taskMAX=500
useLocalCookies=false
myCookies=XXXXXXXXXXXXXXXXXX
schoolName_for_short=njutcm
school_Id=591
Accept=text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9
User-Agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36
';
    $file = fopen("setting.properties", "w") or die("Unable to open file!");
    fwrite($file, $set);
    fclose($file);
    echo "提交出成功请关闭本页面";
    exec('start start.bat');  //linux服务器自行修改
}else{
 echo 'fuck参数错误';

}

function convertUrlQuery($query)
{
    $queryParts = explode('&', $query);

    $params = array();
    foreach ($queryParts as $param)
    {
        $item = explode('=', $param);
        $params[$item[0]] = $item[1];
    }

    return $params;
}

?>

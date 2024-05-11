use std::net::UdpSocket;
use std::str;
use std::process::Command;
fn is_awake_str(str : &str) -> bool{
    return str == "123";
}


fn main() -> std::io::Result<()> {
    // 创建一个UDP Socket并绑定到本地端口51360
    let socket = UdpSocket::bind("0.0.0.0:51360")?;

    loop {
        let mut buf = [0; 1024];
        let (amt, src) = socket.recv_from(&mut buf)?;

        // 将接收到的字节转换为UTF8字符串
        let text = str::from_utf8(&buf[..amt]).expect("Could not write buffer as string");

        // 打印消息和发送者的地址
        println!("Received from {}: {}", src, text);

        if is_awake_str(&text) {
            // 如果是，执行控制台指令echo 123
            Command::new("cmd")
            .arg("/C")
            .arg("/usr/bin/etherwake -D -i \"br-lan\" \"58:11:22:A2:54:23\"")
            .output()
            .expect("Failed to execute command");
            let callback_str = String::from("ServerCallback Successful");
            
            match socket.send_to(callback_str.as_bytes(), src){
                Ok(size)=> println!("Send Callback {}", size),
                Err(e) => println!("Send Callback Failed {}", e)
            }
            
        }
    }
}

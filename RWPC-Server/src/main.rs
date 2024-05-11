use std::net::UdpSocket;
use std::str;

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
    }
}

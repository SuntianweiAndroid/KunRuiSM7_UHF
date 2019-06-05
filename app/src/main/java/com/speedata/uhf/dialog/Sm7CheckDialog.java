package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.serialport.SerialPortBackup;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.bean.SpdReadData;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.utils.DataConversionUtils;
import com.speedata.libuhf.utils.StringUtils;
import com.speedata.uhf.R;
import com.uhf.structures.InventoryData;
import com.uhf.structures.KrSm7Data;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class Sm7CheckDialog extends Dialog implements
        View.OnClickListener {

    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private EditText Write_Passwd, etdkrsm7Pwd, etdkrsm7Action;
    private IUHFService iuhfService;
    private Context mContext;
    private int which_choose;
    private String model;
    private SerialPortBackup serialPort;
    private int fd;
    private String epcs;

    public Sm7CheckDialog(Context context, IUHFService iuhfService, int which_choose) {
        super(context);
        this.iuhfService = iuhfService;
        this.mContext = context;
        this.which_choose = which_choose;
        this.model = model;
        try {
            serialPort = new SerialPortBackup();
            serialPort.OpenSerial(SerialPortBackup.SERIAL_TTYMT1, 38400);
            fd = serialPort.getFd();
        } catch (IOException e) {
            e.printStackTrace();
        }
        iuhfService.setOnReadListener(new OnSpdReadListener() {
            @Override
            public void getReadData(SpdReadData var1) {
                StringBuilder stringBuilder = new StringBuilder();
                byte[] epcData = var1.getEPCData();
                String hexString = StringUtils.byteToHexString(epcData, var1.getEPCLen());
                if (!TextUtils.isEmpty(hexString)) {
                    stringBuilder.append("EPC：" + hexString + "\n");
                }
                if (var1.getStatus() == 0) {
                    byte[] readData = var1.getReadData();
                    String readHexString = StringUtils.byteToHexString(readData, var1.getDataLen());
                    stringBuilder.append("ReadData：" + readHexString + "\n");
                } else {
                    stringBuilder.append("ReadError：" + var1.getStatus() + "\n");
                }
            }
        });
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sm7check_layout);
        Ok = (Button) findViewById(R.id.btn_write_ok);
        Ok.setOnClickListener(this);
        Cancle = (Button) findViewById(R.id.btn_write_cancle);
        Cancle.setOnClickListener(this);
        EPC = (TextView) findViewById(R.id.textView_write_epc);
        Status = (TextView) findViewById(R.id.textView_write_status);
        Write_Passwd = (EditText) findViewById(R.id.editText_write_passwd);
        etdkrsm7Pwd = (EditText) findViewById(R.id.editText_write_krsm7_passwd);
        etdkrsm7Action = (EditText) findViewById(R.id.editText_write_krsm7_action);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Ok) {
            String str_passwd = Write_Passwd.getText().toString();
            String krsm7passwd = etdkrsm7Pwd.getText().toString();
            String krsm7action = etdkrsm7Action.getText().toString();
            if (TextUtils.isEmpty(str_passwd) || TextUtils.isEmpty(krsm7action) || TextUtils.isEmpty(krsm7passwd)) {
                Toast.makeText(mContext, "参数不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            checkFlag(str_passwd, krsm7passwd, krsm7action);
        } else if (v == Cancle) {
            dismiss();
        }
    }

    private boolean checkFlagOld() {
        InventoryData inventoryData = new InventoryData();
        int result = iuhfService.krSm7Inventory(inventoryData);
        if (result == 0) {
            epcs = DataConversionUtils.byteArrayToString(inventoryData.EPC_Data);
            result = iuhfService.selectCard(1, epcs, true);
            if (result == 0) {
//                iuhfService.readArea(2, 0, 6, "00000000");
                byte[] tidDatas = iuhfService.read_area(2, 0, 6, "00000000");//读取tid
                if (tidDatas != null) {
                    String tidDatasStr = DataConversionUtils.byteArrayToString(tidDatas).toUpperCase();
                    if (tidDatasStr.substring(2, 5).equals("0A1")) {
                        Toast.makeText(mContext, "验证成功！", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                Log.e("tw", "选卡失败");
                return false;
            }
        }
        return false;
    }

    public void checkFlag(String pwd, String krsm7pwd, String krsm7action) {
        epcs = "";
//        StringBuilder Status = new StringBuilder();
        InventoryData inventoryData = new InventoryData();
//        int result = iuhfService.krSm7Inventory(inventoryData);
//        if (result == 0) {
//            epcs = DataConversionUtils.byteArrayToString(inventoryData.EPC_Data);
//            EPC.setText(epcs);
//            Status.setText("EPC:" + epcs);
//            //读取tid
//            byte[] tidDatas = iuhfService.read_area(2, 0, 6, pwd);
//            if (tidDatas != null) {
//                String tidDatasStr = DataConversionUtils.byteArrayToString(tidDatas).toUpperCase();
//                String sss = tidDatasStr.substring(2, 5);
//                if (tidDatasStr.substring(2, 5).equals("0A1")) {
//                    Status.append("\nZ型标签验证成功");
//                    EventBus.getDefault().post(new ResultBeen(0, Status.toString(), epcs));
//                } else {
//                    Status.append("\nZ型标签验证失败");
//                    EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
//                }
//            } else {
        epcs = "";
        //重新inventory+selectCard
        int result = iuhfService.krSm7Inventory(inventoryData);
        if (result == 0) {
            epcs = DataConversionUtils.byteArrayToString(inventoryData.EPC_Data);
            sm7check(pwd, krsm7pwd, krsm7action);
            EPC.setText(epcs);
            Status.setText("EPC:" + epcs);
            // 判断sm7check的值，error-验证失败  true-验证成功。
        } else {
            Log.e("tw", "盘点失败");
            Status.append("\n盘点失败请重试");
        }
//            }
//        } else {
//            Log.e("tw", "盘点失败");
//            Status.append("\n盘点失败请重试");
////            sm7check("00000000", "00000000000000000000000000000000", "01");
//        }
    }

    /**
     * 坤锐sm7验证
     *
     * @param pwd
     */
    private void sm7checkOld(String pwd, String krsm7pwd, String krsm7action) {
//        InventoryData inventoryData = new InventoryData();
//        int result = iuhfService.krSm7Inventory(inventoryData);
//        if (result == 0) {
        if (!checkFlagOld()) {
//            String epcs = DataConversionUtils.byteArrayToString(inventoryData.EPC_Data);
//            result = iuhfService.selectCard(1, epcs, true);
//            if (result == 0) {
////                iuhfService.readArea(2, 0, 6, "00000000");
//                byte[] tidDatas = iuhfService.read_area(2, 0, 6, "00000000");//读取tid
//                if (tidDatas != null) {
//                    String tidDatasStr = DataConversionUtils.byteArrayToString(tidDatas);
//                    if (tidDatasStr.substring(2, 5).equals("0A1")) {
//                        Toast.makeText(mContext, "验证成功！", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//
//                }
//            } else {
//                Log.e("tw", "选卡失败");
//                return;
//            }
            EPC.setText(epcs);
            Status.setText("EPC:" + epcs);
            Log.i("twsssss", ": 盘点epc数据" + epcs);
            String test = "0a0a0a0a";
            //blockwrite写uhf-EPC
            String random = DataConversionUtils.byteArrayToString(getRandombyte());
//            String random = "0a0a0a0a";
            int result1 = iuhfService.krSm7Blockwrite(2, 96, 1, DataConversionUtils.HexString2Bytes(pwd), DataConversionUtils.HexString2Bytes(random));
            if (result1 == 0) {
                Status.append("\nToken1：" + random);
                KrSm7Data krSm7Data = new KrSm7Data();
                //sm7读UHF-EPC  读60h区4个word（8个byte）
                result1 = iuhfService.krSm7Read(4, 96, 1, DataConversionUtils.HexString2Bytes(pwd), krSm7Data);
                if (result1 == 0) {
                    Status.append("\nToken1返回：" + DataConversionUtils.byteArrayToString(krSm7Data.Sm7Data));
                    if (krSm7Data.Sm7Data.length != 0) {
                        //解密
//                        String sm7Data = "EE001C060000000000000000000000000000000001"
                        String sm7Data = "EE001C06" + krsm7pwd + krsm7action
                                + DataConversionUtils.byteArrayToString(krSm7Data.Sm7Data).toUpperCase();
                        String crc = Integer.toHexString(CalCRC16(DataConversionUtils.HexString2Bytes(sm7Data)));
                        sm7Data = sm7Data + crc.toUpperCase();
                        Log.i("twsssss", "onClick: " + sm7Data);
                        byte[] sm7DataBytes = DataConversionUtils.HexString2Bytes(sm7Data);
                        Status.append("\nToken2：" + DataConversionUtils.byteArrayToString(sm7DataBytes));
                        serialPort.WriteSerialByte(fd, sm7DataBytes);
                        SystemClock.sleep(300);
                        try {
                            byte[] re = serialPort.ReadSerial(fd, 128);
                            if (re != null) {
                                byte[] status = cutBytes(re, 4, 1);
                                if (DataConversionUtils.byteArrayToInt(status) == 0) {
                                    byte[] statusCode = cutBytes(re, 13, 2);
                                    switch (DataConversionUtils.byteArrayToString(statusCode)) {
                                        case "0000":
                                            //no error
                                            Log.i("sm7", "onClick: no error");
                                            Status.append("\nToken2返回：" + DataConversionUtils.byteArrayToString(re));
                                            byte[] datas = cutBytes(re, 9, 4);
                                            //验证初始随机数 与加密芯片返回的数据是否一样
                                            if (random.equals(DataConversionUtils.byteArrayToString(cutBytes(re, 5, 4)))) {
                                                Status.append("\nToken3：" + DataConversionUtils.byteArrayToString(datas));
                                                result1 = iuhfService.krSm7Blockwrite(4, 97, 1,
                                                        DataConversionUtils.HexString2Bytes(pwd), datas);
                                                if (result1 == 0) {
                                                    Status.append("\n验证成功！！");
//                                                Toast.makeText(this, "验证成功！！", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Status.append("\n验证失败！！");
//                                                Toast.makeText(this, "验证失败！！", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Status.append("\n验证失败！！");
                                            }
                                            break;
                                        case "0001":
                                            //CRC 校验错
                                            Status.append("\n加密芯片返回CRC 校验错");
                                            Log.i("sm7", "onClick: CRC 校验错");
                                            break;
                                        case "0002":
                                            //load key error
                                            Status.append("\n加密芯片返回load key error");
                                            Log.i("sm7", "onClick: load key error");
                                            break;
                                        case "0003":
                                            // 加解密错
                                            Status.append("\n加密芯片返回加解密错");
                                            Log.i("sm7", "onClick: 加解密错");
                                            break;
                                        case "abcd":
                                            //未知错误
                                            Status.append("\n加密芯片返回未知错误");
                                            Log.i("sm7", "onClick: 未知错误");
                                            break;
                                        default:
                                            break;
                                    }
                                } else {
                                    Status.append("\n加密芯片返回错误");
                                    Log.i("sm7", "返回错误");
                                }
                            } else {
                                Status.append("\n串口null");
                            }

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i("sm7", "onClick: 读60h-uhf读返回空");
                    }
                } else {
                    Status.append("\n读60H错误");
                }
            } else {
                Status.append("\n写60H错误");
            }
        } else {
            Status.append("\n盘点错误请重试");
            Log.i("sm7", "盘点错误");
        }
    }

    public void checkFlag2() {
        epcs = "";
        StringBuilder Status = new StringBuilder();
        InventoryData inventoryData = new InventoryData();
        int result = iuhfService.krSm7Inventory(inventoryData);
        if (result == 0) {
            epcs = DataConversionUtils.byteArrayToString(inventoryData.EPC_Data);
            //读取tid
            byte[] tidDatas = iuhfService.read_area(2, 0, 6, "00000000");
            if (tidDatas != null) {
                String tidDatasStr = DataConversionUtils.byteArrayToString(tidDatas).toUpperCase();
                String sss = tidDatasStr.substring(2, 5);
                if (tidDatasStr.substring(2, 5).equals("0A1")) {
                    Status.append("\nZ型标签验证成功");
                    EventBus.getDefault().post(new ResultBeen(0, Status.toString(), epcs));
                } else {
                    Status.append("\nZ型标签验证失败");
                    EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
                }
            } else {
                //重新inventory+selectCard
                result = iuhfService.krSm7Inventory(inventoryData);
                if (result == 0) {
                    sm7check("00000000", "00000000000000000000000000000000", "01");
                    // 判断sm7check的值，error-验证失败  true-验证成功。
                } else {
                    Log.e("tw", "盘点失败");
                    Status.append("\n盘点失败请重试");
                }
            }
        } else {
            Log.e("tw", "盘点失败");
            Status.append("\n盘点失败请重试");
//            sm7check("00000000", "00000000000000000000000000000000", "01");
        }
    }

    /**
     * 坤锐sm7验证
     *
     * @param pwd
     */
    private void sm7check(String pwd, String krsm7pwd, String krsm7action) {
        //blockwrite写uhf-EPC
        String random = DataConversionUtils.byteArrayToString(getRandombyte());
//            String random = "0a0a0a0a";
        int result1 = iuhfService.krSm7Blockwrite(2, 96, 1, DataConversionUtils.HexString2Bytes(pwd), DataConversionUtils.HexString2Bytes(random));
        if (result1 == 0) {
            Status.append("\nToken1：" + random);
            KrSm7Data krSm7Data = new KrSm7Data();
            //sm7读UHF-EPC  读60h区4个word（8个byte）
            result1 = iuhfService.krSm7Read(4, 96, 1, DataConversionUtils.HexString2Bytes(pwd), krSm7Data);
            if (result1 == 0) {
                Status.append("\nToken1返回：" + DataConversionUtils.byteArrayToString(krSm7Data.Sm7Data));
                if (krSm7Data.Sm7Data.length != 0) {
                    //解密
//                        String sm7Data = "EE001C060000000000000000000000000000000001"
                    String sm7Data = "EE001C06" + krsm7pwd + krsm7action
                            + DataConversionUtils.byteArrayToString(krSm7Data.Sm7Data).toUpperCase();
                    String crc = Integer.toHexString(CalCRC16(DataConversionUtils.HexString2Bytes(sm7Data)));
                    sm7Data = sm7Data + crc.toUpperCase();
                    byte[] sm7DataBytes = DataConversionUtils.HexString2Bytes(sm7Data);
                    Status.append("\nToken2：" + DataConversionUtils.byteArrayToString(sm7DataBytes));
                    serialPort.WriteSerialByte(fd, sm7DataBytes);
                    SystemClock.sleep(300);
                    try {
                        byte[] re = serialPort.ReadSerial(fd, 128);
                        if (re != null) {
                            byte[] status = cutBytes(re, 4, 1);
                            if (DataConversionUtils.byteArrayToInt(status) == 0) {
                                byte[] statusCode = cutBytes(re, 13, 2);
                                switch (DataConversionUtils.byteArrayToString(statusCode)) {
                                    case "0000":
                                        //no error
                                        Status.append("\nToken2返回：" + DataConversionUtils.byteArrayToString(re));
                                        byte[] datas = cutBytes(re, 9, 4);
                                        //验证初始随机数 与加密芯片返回的数据是否一样
                                        if (random.equals(DataConversionUtils.byteArrayToString(cutBytes(re, 5, 4)))) {
                                            Status.append("\nToken3：" + DataConversionUtils.byteArrayToString(datas));
                                            result1 = iuhfService.krSm7Blockwrite(4, 97, 1,
                                                    DataConversionUtils.HexString2Bytes(pwd), datas);
                                            if (result1 == 0) {
                                                Status.append("\n验证成功！！");
                                                EventBus.getDefault().post(new ResultBeen(0, Status.toString(), epcs));
                                            } else {
                                                Status.append("\n验证失败！！");
                                                EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
                                            }
                                        } else {
                                            Status.append("\n验证失败！！");
                                            EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
                                        }
                                        break;
                                    case "0001":
                                        //CRC 校验错
                                        Status.append("\n加密芯片返回CRC 校验错");
                                        EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
//                                        Log.i("sm7", "onClick: CRC 校验错");
                                        break;
                                    case "0002":
                                        //load key error
                                        Status.append("\n加密芯片返回load key error");
                                        EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
//                                        Log.i("sm7", "onClick: load key error");
                                        break;
                                    case "0003":
                                        // 加解密错
                                        Status.append("\n加密芯片返回加解密错");
                                        EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
//                                        Log.i("sm7", "onClick: 加解密错");
                                        break;
                                    case "abcd":
                                        //未知错误
                                        Status.append("\n加密芯片返回未知错误");
                                        EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
//                                        Log.i("sm7", "onClick: 未知错误");
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                Status.append("\n加密芯片返回错误");
                                EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
//                                Log.i("sm7", "返回错误");
                            }
                        } else {
                            Status.append("\n串口返回null");
                            EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
//                    Log.i("sm7", "onClick: 读60h-uhf读返回空");
                    EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
                }
            } else {
                Status.append("\n读60H错误");
                EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
            }
        } else {
            Status.append("\n写60H错误");
            EventBus.getDefault().post(new ResultBeen(1, Status.toString(), epcs));
        }

    }

    /**
     * 截取数组
     *
     * @param bytes  被截取数组
     * @param start  被截取数组开始截取位置
     * @param length 新数组的长度
     * @return 新数组
     */
    public static byte[] cutBytes(byte[] bytes, int start, int length) {
        byte[] res = new byte[length];
        System.arraycopy(bytes, start, res, 0, length);
        return res;
    }

    /**
     * crc16校验
     *
     * @param buf
     * @return
     */
    public static char CalCRC16(byte[] buf) {
        char crcSeed = 0xFFFF;
        char crc = crcSeed;
        for (int i = 0; i < buf.length; i++) {
            crc = (char) ((buf[i] << 8) ^ crc);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) == 0x8000) {
                    crc = (char) (crc - 0x8000);
                    crc = (char) ((crc << 1) ^ 0x1021);
                } else {
                    crc <<= 1;
                }
            }
        }
        crc = (char) (crc ^ crcSeed);
        return crc;
    }

    /**
     * 获取随机byte[]
     *
     * @return
     */
    public static byte[] getRandombyte() {
        Random random = new Random();
        byte[] b = {1, 2, 3, 4};
        for (int i = 0; i < 4; i++) {
            Integer is = random.nextInt(9);
            b[i] = Byte.parseByte(is.toString());
        }
        return b;
    }
}

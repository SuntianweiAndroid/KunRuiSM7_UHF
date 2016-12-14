package com.speedata.libuhf;


import android.os.Handler;
import android.os.Message;

import com.pow.api.cls.RfidPower;
import com.uhf.api.cls.Reader;

import java.util.ArrayList;
import java.util.List;

/**
 * 旗连芯片  芯联方案
 * Created by 张明_ on 2016/11/29.
 */
public class XinLianQilian implements IUHFService {

    private static Reader Mreader = new Reader();
    private static int antportc;
    private static RfidPower.PDATYPE PT = RfidPower.PDATYPE.valueOf(19);
    private static RfidPower Rpower = new RfidPower(PT);
    private Handler handler_inventer = null;
    private ReaderParams Rparams = new ReaderParams();
    private int ThreadMODE = 0;
    private Handler handler = new Handler();
    public boolean nostop = false;
    Reader.TagFilter_ST g2tf = null;
    public static String TID;


    //初始化模块
    public int OpenDev() {
        try {
            boolean blen = Rpower.PowerUp();
            if (!blen)
                return -1;
            Reader.READER_ERR er = Mreader.InitReader_Notype("/dev/ttyMT2", 1);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                antportc = 1;
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    //关闭模块
    public void CloseDev() {
        if (Mreader != null)
            Mreader.CloseReader();
        Rpower.PowerDown();
    }

    //注册过 Handler 后调用此函数开始盘点过程
    public void inventory_start() {
        handler.postDelayed(inv_thread, 0);
    }

    @Override
    public void inventory_start(Handler hd) {
        reg_handler(hd);
        inventory_start();
    }

    //停止盘点。
    public void inventory_stop() {
        handler.removeCallbacks(inv_thread);
    }

    //从标签 area 区的 addr 位置（以 word 计算）读取 count 个值（以 byte 计算）
    // passwd 是访问密码，如果区域没被锁就给 0 值。
    public byte[] read_area(int area, int addr, int count, int passwd) {
        try {
            byte[] rdata = new byte[count * 2];
            byte[] rpaswd = new byte[4];
            String pwd = String.valueOf(passwd);
            if (!pwd.equals("")) {
                Mreader.Str2Hex(pwd, pwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.GetTagData(Rparams.opant,
                        (char) area, addr, count,
                        rdata, rpaswd, (short) Rparams.optime);

                trycount--;
                if (trycount < 1)
                    break;
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                return rdata;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    //把 content 中的数据写到标签 area 区中 addr（以 word 计算）开始的位 置。
    public int write_area(int area, int addr, int passwd, byte[] content) {
        try {
            byte[] rpaswd = new byte[4];
            String pwd = String.valueOf(passwd);
            if (!pwd.equals("")) {
                Mreader.Str2Hex(pwd, pwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.WriteTagData(Rparams.opant,
                        (char) area, addr, content, content.length, rpaswd,
                        (short) Rparams.optime);
                trycount--;
                if (trycount < 1)
                    break;
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    //选中要进行操作的 epc 标签
    public int select_card(byte[] epc) {
        g2tf = Mreader.new TagFilter_ST();
        g2tf.fdata = epc;
        g2tf.flen = epc.length * 8;
        g2tf.isInvert = 0;
        g2tf.bank = 1;
        g2tf.startaddr = 32;
        Reader.READER_ERR er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
        if (er != Reader.READER_ERR.MT_OK_ERR) {
            return -1;
        }
        return 0;
    }

    //设置天线功率
    public int set_antenna_power(int power) {
        Reader.AntPowerConf apcf = Mreader.new AntPowerConf();
        apcf.antcnt = antportc;
        Reader.AntPower jaap = Mreader.new AntPower();
        jaap.antid = 1;
        jaap.readPower = (short) (power * 100);
        jaap.writePower = (short) (power * 100);
        apcf.Powers[0] = jaap;
        try {
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    //读取当前天线功率值
    public int get_antenna_power() {
        int rv = 0;
        try {
            Reader.AntPowerConf apcf2 = Mreader.new AntPowerConf();
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                for (int i = 0; i < apcf2.antcnt; i++) {
                    if (i == 0) {
                        rv = (apcf2.Powers[i].readPower) / 100;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return rv;
    }

    //设定区域锁定状态。
    public int setlock(int type, int area, int passwd) {
        try {
            Reader.Lock_Obj lobj = null;
            Reader.Lock_Type ltyp = null;
            if (area == 0) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD;
                if (type == 0)
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_LOCK;
                else if (type == 2)
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_PERM_LOCK;

            } else if (area == 1) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD;
                if (type == 0)
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_LOCK;
                else if (type == 2)
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_PERM_LOCK;
            } else if (area == 2) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK1;
                if (type == 0)
                    ltyp = Reader.Lock_Type.BANK1_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.BANK1_LOCK;
                else if (type == 2)
                    ltyp = Reader.Lock_Type.BANK1_PERM_LOCK;
            } else if (area == 3) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK2;
                if (type == 0)
                    ltyp = Reader.Lock_Type.BANK2_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.BANK2_LOCK;
                else if (type == 2)
                    ltyp = Reader.Lock_Type.BANK2_PERM_LOCK;
            } else if (area == 4) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK3;
                if (type == 0)
                    ltyp = Reader.Lock_Type.BANK3_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.BANK3_LOCK;
                else if (type == 2)
                    ltyp = Reader.Lock_Type.BANK3_PERM_LOCK;
            }
            byte[] rpaswd = new byte[4];
            String pwd = String.valueOf(passwd);
            if (!pwd.equals("")) {
                Mreader.Str2Hex(pwd, pwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Mreader.LockTag(Rparams.opant,
                    (byte) lobj.value(), (short) ltyp.value(),
                    rpaswd, (short) Rparams.optime);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    //设置频率区域
    public int set_freq_region(int region) {
        try {
            Reader.Region_Conf rre;
            switch (region) {
                case 0:
                    rre = Reader.Region_Conf.RG_PRC;
                    break;
                case 1:
                    rre = Reader.Region_Conf.RG_NA;
                    break;
                case 2:
                    rre = Reader.Region_Conf.RG_NONE;
                    break;
                case 3:
                    rre = Reader.Region_Conf.RG_KR;
                    break;
                case 4:
                    rre = Reader.Region_Conf.RG_EU;
                    break;
                case 5:
                    rre = Reader.Region_Conf.RG_EU2;
                    break;
                case 6:
                    rre = Reader.Region_Conf.RG_EU3;
                    break;
                case 7:
                case 8:
                case 9:
                case 10:
                default:
                    rre = Reader.Region_Conf.RG_NONE;
                    break;
            }
            if (rre == Reader.Region_Conf.RG_NONE) {
                return -1;
            }
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rre);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public int get_freq_region() {
        try {
            Reader.Region_Conf[] rcf2 = new Reader.Region_Conf[1];
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rcf2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                switch (rcf2[0]) {
                    case RG_PRC:
                        return 0;
                    case RG_EU:
                        return 4;
                    case RG_EU2:
                        return 5;
                    case RG_EU3:
                        return 6;
                    case RG_KR:
                        return 3;
                    case RG_NA:
                        return 1;
                    default:
                        return 7;
                }
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    private Runnable inv_thread = new Runnable() {
        public void run() {
            String[] tag = null;
            int[] tagcnt = new int[1];
            tagcnt[0] = 0;
            synchronized (this) {
                Reader.READER_ERR er;
                if (nostop) {
                    er = Mreader.AsyncGetTagCount(tagcnt);
                } else {
                    int[] uants = Rparams.uants;
                    er = Mreader.TagInventory_Raw(uants,
                            Rparams.uants.length,
                            (short) Rparams.readtime, tagcnt);
                }
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    if (tagcnt[0] > 0) {
                        tag = new String[tagcnt[0]];
                        for (int i = 0; i < tagcnt[0]; i++) {
                            Reader.TAGINFO tfs = Mreader.new TAGINFO();
                            if (Rpower.GetType() == RfidPower.PDATYPE.SCAN_ALPS_ANDROID_CUIUS2) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            if (nostop)
                                er = Mreader.AsyncGetNextTag(tfs);
                            else
                                er = Mreader.GetNextTag(tfs);

                            if (er == Reader.READER_ERR.MT_OK_ERR) {
                                tag[i] = Reader.bytes_Hexstr(tfs.EpcId);
                                byte[] n_epc = tag[i].getBytes();
                                ArrayList<Tag_Data> cx = new ArrayList<Tag_Data>();
                                cx.add(new Tag_Data(null, n_epc));
                                Message msg = new Message();
                                if (cx != null) {
                                    msg.what = 1;
                                    msg.obj = cx;
                                    handler_inventer.sendMessage(msg);
                                }
                            }
                        }
                    }

                } else {
                    if (nostop && er != Reader.READER_ERR.MT_OK_ERR) {
                        handler.removeCallbacks(inv_thread);
                    }
                    if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                        inventory_stop();
                    } else
                        handler.postDelayed(this, Rparams.sleep);
                    return;
                }
            }
            handler.postDelayed(this, Rparams.sleep);
        }
    };

    public class Tag_Data {
        Tag_Data(byte[] n_tid, byte[] n_epc) {
            tid = n_tid;
            epc = n_epc;
        }

        public byte[] tid;
        public byte[] epc;
    }

    //用户需要自己实现一个 Handler，然后用些函数向 API 注册，然后才可以 开始盘点。
    // 盘点到的 EPC 等数据会通过向注册的 Handler 发消息 （Message）的方式来实现。
    // 当搜索到有效 EPC 和 TID 数据后，Handler 会收到 Message，其中 Meseage.what 值为 1，
    // Message.obj 就是保存了 EPC 数据的 Tag_Data类的ArrayList。
    // Tag_Data 类有两个byte[ ]型的public 成员，一个是 epc，一个是 tid，如果为 null，
    // 代表对应的值不存在。
    public void reg_handler(Handler hd) {
        handler_inventer = hd;
    }


    public class ReaderParams {

        //save param
        public int opant;

        public List<String> invpro;
        public String opro;
        public int[] uants;
        public int readtime;
        public int sleep;

        public int checkant;
        public int[] rpow;
        public int[] wpow;

        public int region;
        public int[] frecys;
        public int frelen;

        public int session;
        public int qv;
        public int wmode;
        public int blf;
        public int maxlen;
        public int target;
        public int gen2code;
        public int gen2tari;

        public String fildata;
        public int filadr;
        public int filbank;
        public int filisinver;
        public int filenable;

        public int emdadr;
        public int emdbytec;
        public int emdbank;
        public int emdenable;

        public int antq;
        public int adataq;
        public int rhssi;
        public int invw;
        public int iso6bdeep;
        public int iso6bdel;
        public int iso6bblf;
        public int option;
        //other params

        public String password;
        public int optime;

        public ReaderParams() {
            opant = 1;
            invpro = new ArrayList<String>();
            invpro.add("GEN2");
            uants = new int[1];
            uants[0] = 1;
            sleep = 0;
            readtime = 50;
            optime = 1000;
            opro = "GEN2";
            checkant = 1;
            rpow = new int[]{2700, 2000, 2000, 2000};
            wpow = new int[]{2000, 2000, 2000, 2000};
            region = 1;
            frelen = 0;
            session = 0;
            qv = -1;
            wmode = 0;
            blf = 0;
            maxlen = 0;
            target = 0;
            gen2code = 2;
            gen2tari = 0;

            fildata = "";
            filadr = 32;
            filbank = 1;
            filisinver = 0;
            filenable = 0;

            emdadr = 0;
            emdbytec = 0;
            emdbank = 1;
            emdenable = 0;

            adataq = 0;
            rhssi = 1;
            invw = 0;
            iso6bdeep = 0;
            iso6bdel = 0;
            iso6bblf = 0;
            option = 0;
        }
    }
}
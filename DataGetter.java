import processing.core.*; import processing.bluetooth.*; public class DataGetter extends PMIDlet{// DataGetter
// by C. K. Harnett, c0harn01@gwise.louisville.edu
//
//
//



//these softkeys appear at the bottom of the screen at various times
//activated by pressing the phone's "enter" button-- usually in the center of phone's control buttons

final String SOFTKEY_BEGIN = "Begin"; //softkey to start data acquisition
final String SOFTKEY_END = "End";  //softkey to stop data acquisition
final String SOFTKEY_RETRY = "Retry?"; //softkey to throw out previous data and try again

// state machine sates
final int STATE_START    = 0;
final int STATE_FIND     = 1;
final int STATE_READY    = 2;
final int STATE_READING  = 3;


// state machine state var
int state;

//variable reporting length of latest captured data
int filelength =0;  

//variable containing received data - expands and contracts accordingly
byte[] dataToSave = new byte[16];

// bluetooth library
Bluetooth bt;
// discovered services
Service[] services;
// status message
String msg;
// connection to Bluetooth modem
Client c;

PFont font;


///
public void setup() {
  font = loadFont();
  textFont(font);
  bt = new Bluetooth(this, Bluetooth.UUID_SERIALPORT);

  ellipseMode(CORNER);

  state = STATE_START;
}

public void destroy() {
  bt.stop();
}

//
public void draw() {
  background(0xcc,0xcc,0xff);

  if (state == STATE_START) {
    fill(0);
    textAlign(LEFT);
    text("DataGetter\n\nPress a key to search for sensors", 2, 2, width - 4, height - 4);
  } 
  else if (state == STATE_FIND) {
    fill(0);
    textAlign(LEFT);
    if (services == null) {
      text("Looking for sensors...\n\n", 2, 2, width-4, height-4);
    }
    else {
      String list = "Select a Device:\n";
     // list += length(services)+"\n";
      for (int i = 0, len = length(services); i < len; i++){
        list += i + ". " + services[i].device.name + "\n";
      }
      text(list, 2, 2, width-4, height-4);
    }
  } 
  else if (state == STATE_READY) {//In "READY" state we are ready to go to "READING" state for data acq, or are storing data from a read.
    noFill();
    text("Connected:\n"+msg, 2, 2, width-4, height-4);
    while(c.available()>0)
    {
    c.read(); //Read and discard any bytes arriving outside of data acquisition period
    }
  }
  else if (state == STATE_READING) {//Data acquisition occurs only in this state
    noFill();
    byte[] inBytes= new byte[1];
    while (c.available()>0)
            {
             c.readBytes(inBytes);
             for (int k=0;k<inBytes.length;k+=1){
                 filelength=filelength+1;
                 if (filelength==dataToSave.length) {
                 dataToSave = expand(dataToSave,dataToSave.length+inBytes.length); //grow the array if needed
              }
                 dataToSave[filelength]=inBytes[k];
                 msg=msg+" "+inBytes[k];//Display data: bytes are printed as decimal numbers. Modify here if you would rather see characters or hex.
             }    
            }           
    text("Connected: reading\n" +msg,2,2,width-4,height-4);  
  }
  
}

//
public void libraryEvent(Object library, int event, Object data) {
  if (library == bt) {
    switch (event) {
    case Bluetooth.EVENT_DISCOVER_DEVICE:
      msg = "Found device at: " + ((Device) data).address + "...";
      break;
    case Bluetooth.EVENT_DISCOVER_DEVICE_COMPLETED:
      msg = "Found " + length((Device[]) data) + " devices, looking for serial port...";
      break;
    case Bluetooth.EVENT_DISCOVER_SERVICE:      
      msg = "Found serial port on " + ((Service[]) data)[0].device.address + "...";
      break;
    case Bluetooth.EVENT_DISCOVER_SERVICE_COMPLETED:
      services = (Service[]) data;
      msg = "Search complete. Pick one.";
      break;
    case Bluetooth.EVENT_CLIENT_CONNECTED:
      c = (Client) data;
      msg = "Client connected."; 
      break;  
    }
  }
}

//
public void softkeyPressed(String label) {
  if (label.equals(SOFTKEY_BEGIN)) {//user wants to start data acquisition
    filelength=0;  //reset filelength in case user is attempting to collect new data after hitting "Retry" button
    softkey(SOFTKEY_END); //enable user to stop acquisition
    dataToSave = contract(dataToSave,16);  //re-shrink array down to 16 again whether or not it's been used;
    msg="";//reset message from whatever it was before
    state=STATE_READING; //go to data acq mode

    }
  
  else if(label.equals(SOFTKEY_END)) {//user wants to stop data acq
    state=STATE_READY;//go back to data storage/ready to acq mode
    saveBytes("mybytes", dataToSave);  //this puts bytes in a file on the phone in the same directory as the application
    msg=filelength+" bytes saved to\n DataGetter_m_mybytes.rms";//open in text editor to see characters or hex editor to see bytes
    softkey(SOFTKEY_RETRY);//user can go back to acquire new data if desired

  }
  else if (label.equals(SOFTKEY_RETRY)){ 
    msg="Ready to collect data?";
    softkey(SOFTKEY_BEGIN);
  }
}

//
public void keyPressed() {
  if (state == STATE_START) {
    services = null;
    bt.find();
    state = STATE_FIND;

  } 
  else if (state == STATE_FIND) {//user has selected a device from the list--connect to it and go to READY state
    if (services != null) {
      if ((key >= '0') && (key <= '9')) {
        int i = key - '0';
        if (i < length(services)) {
          msg = "connecting...";
          c = services[i].connect();
          msg = "Ready to collect data?";
          state = STATE_READY;
          softkey(SOFTKEY_BEGIN);
        }
      }
    }
  }
 
 

}




 

                                                                                                                  

}
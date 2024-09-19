/*******************************************************************************
 * Copyright 2019 Observational Health Data Sciences and Informatics
 * 
 * This file is part of WhiteRabbit
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.utilities.files;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReadCsvFile extends ReadTextFile{
  public String filename;
  protected BufferedReader bufferedReader;
  public boolean EOF = false;
  public long charCount;
  public long fileSize;
  public int splits;
  public int currentSplit;
  public long linesRead;
  public int sampleSize;
  public int charSkipped;

  public ReadCsvFile(String filename, int sampleSize, int splits) {
    super(filename);
    this.filename = filename;
    try {
      FileInputStream inputStream = new FileInputStream(filename);
      bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      System.err.println("Computer does not support UTF-8 encoding");
      e.printStackTrace();
    }

    this.sampleSize = sampleSize;
    this.currentSplit = 0;
    this.linesRead = 0;
    this.charCount = 0;
    this.charSkipped = 0;
    this.splits = splits;

    try {
      this.fileSize = Files.size(Paths.get(filename));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  

  public Iterator<String> getIterator() {
    return iterator();
  }

  public List<String> loadFromFileInBatches(Integer batchsize) {
    List<String> result = new ArrayList<String>();
    if (!EOF) {
      try {
        int i = 0;
        while (!EOF && i++ < batchsize) {
          String nextLine = bufferedReader.readLine();
          if (nextLine == null)
            EOF = true;
          else
            result.add(nextLine);
        }
        if (EOF) {
          bufferedReader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  private class CsvFileIterator implements Iterator<String> {
    private String buffer;
    
    public CsvFileIterator() {
      try {
        buffer = bufferedReader.readLine();
        if(buffer == null | linesRead > sampleSize) {
          EOF = true;
          bufferedReader.close();
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }

    }

    public boolean hasNext() {
      return !EOF;
    }

    public String next() {
      String result = buffer;
      try {
        if (linesRead > 0 && linesRead % (sampleSize/splits) == 0){
          currentSplit ++;

          int nextStartLine = (getLineCountEstimate()/splits)*currentSplit;

          int expectedLinesSkipped = (int) ((charSkipped * linesRead) /charCount);
          long charsToSkip = ((nextStartLine - (linesRead + expectedLinesSkipped)) * (charCount/linesRead));

          charSkipped += (int) bufferedReader.skip(charsToSkip);
          bufferedReader.readLine();
        }
        buffer = bufferedReader.readLine();
        if(buffer == null) {
          EOF = true;
          bufferedReader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      // length + 2 to account for \n characters
      charCount += result.length() + 2;
      linesRead ++;
      return result;
    }

    public void remove() {
      // not implemented
    }

  }

  public Iterator<String> iterator() {
    return new CsvFileIterator();
  }

  public int getLineCountEstimate() {
    return (int) ((fileSize * linesRead) / charCount);
  }
}

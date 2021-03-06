/*
 * Copyright 2016 The BigDL Authors.
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
 */

package com.intel.analytics.zoo.pipeline.common

import java.io.File

import com.intel.analytics.bigdl.nn._
import com.intel.analytics.zoo.pipeline.common.caffe.{CaffeLoader, FrcnnCaffeLoader, SSDCaffeLoader}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.{Engine, T}
import org.apache.spark.SparkContext
import org.scalatest.{FlatSpec, Matchers}

class CaffeLoaderSpec extends FlatSpec with Matchers {
  val home = System.getProperty("user.home")


  def compare(name: String, t1: Tensor[Float], t2: Tensor[Float]): Boolean = {
    if (t1.toTensor[Float].nElement() != t2.toTensor[Float].nElement()) {
      println(s"compare ${name} fail, ${t1.toTensor[Float].nElement()} vs" +
        s" ${t2.toTensor[Float].nElement()}")
      return false
    }
    t1.toTensor[Float].map(t2.toTensor[Float],
      (a, b) => {
        if (Math.abs(a - b) > 1e-6) {
          println(s"compare $name fail")
          return false
        }
        a
      })
    true
  }




  "deepbit model load" should "work properly" in {
    val prototxt = s"$home/Downloads/models/deploy16.prototxt"
    val caffemodel = s"$home/Downloads/models/DeepBit16_final_iter_1.caffemodel"

    if (!new File(prototxt).exists()) {
      cancel("local test")
    }
    val model = SSDCaffeLoader.loadCaffe(prototxt, caffemodel)

    ModuleUtil.shareMemory(model)
    model.evaluate()
    val input = Tensor[Float](1, 3, 224, 224)
    val output = model.forward(input)
    println(output.toTensor[Float].size().mkString("x"))
  }

  "deepbit model load with caffe input" should "work properly" in {
    val prototxt = s"$home/Downloads/models/deploy16.prototxt"
    val caffemodel = s"$home/Downloads/models/DeepBit16_final_iter_1.caffemodel"

    if (!new File(prototxt).exists()) {
      cancel("local test")
    }
    TestUtil.middleRoot = s"$home/data/deepbit"
    val input = TestUtil.loadFeaturesFullPath(s"$home/data/deepbit/data-2_3_224_224.txt")
    val model = CaffeLoader.loadCaffe[Float](prototxt, caffemodel)._1

    ModuleUtil.shareMemory(model)
    model.evaluate()
    val output = model.forward(input).toTensor[Float]
    val namedModule = Utils.getNamedModules(model)
    TestUtil.assertEqual("fc8_kevin", namedModule("fc8_kevin").output.toTensor, 1e-5)
    println(output.toTensor[Float].size().mkString("x"))
  }

  "deepbit 1.0 model load with caffe input" should "work properly" in {
    val prototxt = s"$home/Downloads/models/deepbit_1.0.prototxt"
    val caffemodel = s"$home/Downloads/models/deepbit_1.0.caffemodel"

    if (!new File(prototxt).exists()) {
      cancel("local test")
    }
    TestUtil.middleRoot = s"$home/data/deepbit/1.0"
    val input = TestUtil.loadFeatures("data")
    //    val input = Tensor[Float](1, 3, 227, 227)
    val model = CaffeLoader.loadCaffe[Float](prototxt, caffemodel)._1

    ModuleUtil.shareMemory(model)
    model.evaluate()
    val output = model.forward(input).toTensor[Float]
    val namedModule = Utils.getNamedModules(model)
    TestUtil.assertEqual("fc8_kevin", namedModule("fc8_kevin").output.toTensor, 1e-7)
    println(output.toTensor[Float].size().mkString("x"))
  }

  "fasterrcnn vgg load with caffe" should "work properly" in {
    val conf = Engine.createSparkConf().setMaster("local[2]")
      .setAppName("Spark-DL Faster RCNN Test")
    val sc = new SparkContext(conf)
    Engine.init

    val prototxt = s"$home/data/caffeModels/vgg16/test.prototxt"
    val caffemodel = s"$home/data/caffeModels/vgg16/test.caffemodel"

    if (!new File(prototxt).exists()) {
      cancel("local test")
    }
    val model = FrcnnCaffeLoader.loadCaffe(prototxt, caffemodel)
    val input = T()
    input.insert(Tensor[Float](1, 3, 60, 90))
    input.insert(Tensor[Float](T(60, 90, 1, 1)).resize(1, 4))
    model.saveModule("/tmp/vgg.frcnn", true)
    model.forward(input)
  }

  "pvanet load" should "work properly" in {

    val conf = Engine.createSparkConf().setMaster("local[2]")
      .setAppName("Spark-DL Faster RCNN Test")
    val sc = new SparkContext(conf)
    Engine.init
    val prototxt = s"$home/data/caffeModels/pvanet/test.prototxt"
    val caffemodel = s"$home/data/caffeModels/pvanet/PVA9.1_ImgNet_COCO_VOC0712.caffemodel"

    if (!new File(prototxt).exists()) {
      cancel("local test")
    }
    val model = FrcnnCaffeLoader.loadCaffe(prototxt, caffemodel)
    val input = T()
    input.insert(Tensor[Float](1, 3, 640, 960))
    input.insert(Tensor[Float](T(640, 960, 1, 1)).resize(1, 4))
    println("save model done")

    model.saveModule("/tmp/pvanet.model", true)
    model.forward(input)
  }

  "pvanet forward" should "work properly" in {
    val conf = Engine.createSparkConf().setMaster("local[2]")
      .setAppName("Spark-DL Faster RCNN Test")
    val sc = new SparkContext(conf)
    Engine.init
    val prototxt = s"$home/data/caffeModels/pvanet/faster_rcnn_train_test_21cls.pt"
    val caffemodel = s"$home/data/caffeModels/pvanet/PVA9.1_ImgNet_COCO_VOC0712.caffemodel"

    if (!new File(prototxt).exists()) {
      cancel("local test")
    }
    val input = T()
    input.insert(TestUtil.loadFeaturesFullPath(s"$home/data/middle/pvanew/data-1_3_640_960.txt"))
    input.insert(Tensor[Float](T(640, 960, 1.9199999, 1.9199999)).resize(1, 4))
    val modelWithPostprocess = Module.loadModule[Float]("/tmp/pvanet.model")
    modelWithPostprocess.evaluate()
    println("load done !")
    TestUtil.middleRoot = s"$home/data/middle/pvanew/"
    modelWithPostprocess.forward(input)
    def assertEqual(name: String): Unit = {
      TestUtil.assertEqual(name, modelWithPostprocess(name).get.output.toTensor[Float], 1e-4)
    }
    def assertEqual2(name: String, name2: String): Unit = {
      TestUtil.assertEqual(name, modelWithPostprocess(name2).get.output.toTensor[Float], 1e-4)
    }
    assertEqual2("conv1_1_conv", "conv1_1/bn")
    assertEqual("pool1")
    assertEqual2("conv2_1_proj", "conv2_1/proj")
    assertEqual2("conv2_1_3", "conv2_1/3/conv")
    assertEqual("conv2_1")
    assertEqual("conv3_4")
    assertEqual("conv4_4")
    assertEqual("concat")
    assertEqual("upsample")
    assertEqual("downsample")
    assertEqual("roi_pool_conv5")
    assertEqual2("rois", "proposal")
    TestUtil.assertEqual("cls_prob",
      modelWithPostprocess("cls_prob").get.output.toTensor[Float], 1e-5)
    TestUtil.assertEqual("bbox_pred",
      modelWithPostprocess("bbox_pred").get.output.toTensor[Float], 1e-5)
  }
}

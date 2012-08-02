/******************************************************************************
* File:              TDSLogViewer.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.JPopupMenu;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.event.*;
import org.jfree.data.Range;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import java.io.*;
import java.util.*;


//import org.jfree.ui.RefineryUtilities;

public class TDSLogViewer   extends JFrame
                            implements ActionListener
{
    public static final String EXIT_COMMAND = "EXIT";

    private JPanel mainPanel;
    private JPanel graphViewPanel;
    private JPanel logSelectorPanel;
    private JPanel parameterSelectorPanel;
    private JPanel dataViewPanel;

    private JFreeChart mainChart;
    
    public TDSLogViewer()
    {
        super("TDS-1 Log Viewer");
        
        setJMenuBar(createMenuBar());
        
        graphViewPanel = createGraphViewPanel();
        logSelectorPanel = createLogSelectorPanel();
        parameterSelectorPanel = createParameterSelectorPanel();
        dataViewPanel = createDataViewPanel();
        
        mainPanel = new JPanel(new BorderLayout());
        
        JSplitPane hsplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.add(hsplitter);
        
        JPanel right = new JPanel(new BorderLayout());
        hsplitter.setRightComponent(right);
        
        JPanel left  = new JPanel(new BorderLayout());
        hsplitter.setLeftComponent(left);

        JSplitPane rvsplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        right.add(rvsplitter);
        rvsplitter.setTopComponent(graphViewPanel);
        rvsplitter.setBottomComponent(dataViewPanel);

        JSplitPane lvsplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        left.add(lvsplitter);
        lvsplitter.setTopComponent(logSelectorPanel);
        lvsplitter.setBottomComponent(parameterSelectorPanel);
        
        setContentPane(mainPanel);
    }

    private JScrollBar chartScrollBar;

    public class ChartScrollListener implements AxisChangeListener, AdjustmentListener
    {
        private JScrollBar scrollBar;
        private XYPlot xyPlot;
        MultiAxisSampleLog ml;
        public ChartScrollListener(JScrollBar sb, XYPlot plot,
                MultiAxisSampleLog m)
        {
            scrollBar = sb;
            xyPlot = plot;
            ml = m;

            scrollBar.addAdjustmentListener(this);
        }

        public void adjustmentValueChanged(AdjustmentEvent event)
        {
            Range pr = xyPlot.getDomainAxis().getRange();
            Double viewMin = pr.getLowerBound();
            Double viewMax = pr.getUpperBound();

            Double newMax = new Double(scrollBar.getValue()) + (viewMax-viewMin);
            Double newMin = new Double(scrollBar.getValue());

            System.out.format("adjustmentValueChanged: was viewmin/max %f, %f now %f, %f\n"+
                              "                        scrollBar.getValue() = %d\n",
                              viewMin, viewMax, newMin, newMax, scrollBar.getValue());

            xyPlot.getDomainAxis().setRange(new Range(newMin, newMax));
        }

        public void axisChanged(AxisChangeEvent event)
        {
            System.out.format("Axis changed\n");

            setScrollRange();
        }

        private Double lastViewMin, lastViewMax;
        private void setScrollRange()
        {
            Range pr = xyPlot.getDomainAxis().getRange();
            Double viewMin = pr.getLowerBound();
            Double viewMax = pr.getUpperBound();

            if (viewMin == lastViewMin && viewMax == lastViewMax)
            {
                System.out.format("No viewport change\n");
                return;
            }

            if (ml == null)
            {
                System.out.format("No multiAxis in setScrollRange\n");
                return;
            }

            Range r = ml.GetTimeRange();

            scrollBar.setValues(viewMin.intValue(), 
                    viewMax.intValue()-viewMin.intValue(), 
                    new Double(r.getLowerBound()).intValue(), 
                    new Double(r.getUpperBound()).intValue());

            System.out.format("scrollbar.setValues(%d, %d, %d, %d)\n",
                    viewMin.intValue(), 
                    viewMax.intValue()-viewMin.intValue(), 
                    new Double(r.getLowerBound()).intValue(), 
                    new Double(r.getUpperBound()).intValue());

        }

    }
    private ChartScrollListener chartScrollListener;

    private void bindScrollbarToRange(MultiAxisSampleLog ml)
    {
        XYPlot plot = (XYPlot) mainChart.getPlot();
        chartScrollListener = new ChartScrollListener(chartScrollBar, plot,
               ml);
        plot.getRangeAxis().addChangeListener(chartScrollListener);
    }

    private JPanel createGraphViewPanel()
    {
        mainChart = createChart();

        chartScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, 0);

        ChartPanel cpanel = new ChartPanel(mainChart);
        cpanel.addChartMouseListener(new TDSChartMouseListener(cpanel, this));
        cpanel.setPreferredSize(new java.awt.Dimension(1200, 800));
        
        Box vbox = new Box(BoxLayout.Y_AXIS);
        vbox.add(cpanel);        
        vbox.add(chartScrollBar);
        
        JPanel gvpanel = new JPanel();
        gvpanel.setLayout(new BorderLayout());
        gvpanel.add(vbox);
        return gvpanel;
    }

    private JPanel createLogSelectorPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        return panel;            
    }
    
    private LogTreeNode LogTreeRoot = new LogTreeNode();
    {
        LogTreeRoot.setText("Data Logs");
    }
    private JTree logPanelTree = new JTree(new DefaultTreeModel(LogTreeRoot));
    
    private void treeClickHandler(MouseEvent e)
    {
        TreePath selPath = logPanelTree.getPathForLocation(e.getX(), e.getY());
        
        if (e.isPopupTrigger())
        {
            // make the right-clicked node selected
            logPanelTree.setSelectionPath(selPath);

            JPopupMenu menu = new JPopupMenu();
            
            final LogTreeNode node = (LogTreeNode)selPath.getLastPathComponent();
            
            if (node.isAxis())
            {
                final SingleAxisSampleLog sl = node.getSingleLog();
                JMenuItem chgcolor = new JMenuItem("Plot color");
                if (sl.getRenderer().isSeriesVisible(0))
                {
                    JMenuItem hide = new JMenuItem("Hide Axis");
                    hide.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    sl.getAxis().setVisible(false);
                                    sl.getRenderer().setSeriesVisible(0, false);
                                }
                            }
                        );
                    menu.add(hide);
                }
                else
                {
                    JMenuItem show = new JMenuItem("Show Axis");
                    show.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    sl.getAxis().setVisible(true);
                                    sl.getRenderer().setSeriesVisible(0, true);
                                }
                            }
                        );
                    menu.add(show);
                }
                    
                if (sl.getRenderer().getSeriesShapesVisible(0))
                {
                    JMenuItem hideshapes = new JMenuItem("Hide Points");
                    hideshapes.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    sl.getRenderer().setSeriesShapesVisible(0, false);
                                    System.out.format("Turned shapes visible off\n");
                                }
                            }
                        );
                    menu.add(hideshapes);
                }
                else
                {
                    JMenuItem showshapes = new JMenuItem("Show Shapes");
                    showshapes.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    sl.getRenderer().setSeriesShapesVisible(0, true);
                                    System.out.format("Turned shapes visible on\n");
                                }
                            }
                        );
                    menu.add(showshapes);
                }

                if (sl.getAxis().getAutoRangeIncludesZero())
                {
                    JMenuItem hidezero = new JMenuItem("No Zero Crossing Include");
                    hidezero.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    sl.getAxis().setAutoRangeIncludesZero(false);
                                    sl.getAxis().setAutoRange(true);
                                }
                            }
                        );
                    menu.add(hidezero);
                }
                else
                {
                    JMenuItem showzero = new JMenuItem("Zero Crossing Include");
                    showzero.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    sl.getAxis().setAutoRangeIncludesZero(true);
                                    sl.getAxis().setAutoRange(true);
                                }
                            }
                        );
                    menu.add(showzero);
                }

                JMenuItem clone = new JMenuItem("Clone");
                clone.addActionListener(new
                        ActionListener()
                        {
                            public void actionPerformed(ActionEvent event)
                            {
                                String s = (String)JOptionPane.showInputDialog(
                                                    null, // frame
                                                    "Enter name of new axis\n",
                                                    "Clone Axis",
                                                    JOptionPane.PLAIN_MESSAGE,
                                                    null, // icon
                                                    null,
                                                    "Copy of " + sl.GetName());

                                if ((s != null) && (s.length() > 0)) {
                                    
                                    SingleAxisSampleLog snew = new SingleAxisSampleLog(sl);
                                    snew.SetName(s);
                                    addSingleAxisSampleLogToChart(snew, nextDataset, (XYPlot)mainChart.getPlot());
                                    snew.setDataset(nextDataset);
                                    ++nextDataset;
                                    LogTreeNode snode = new LogTreeNode(snew);
                                    snode.setText(snew.GetName());
                                    snew.setLogTreeNode(snode);
                                    sl.getLogTreeNode().add(snode);
                                }

                            }
                        }
                    );
                menu.add(clone);

                JMenuItem filter = new JMenuItem("Lowpass Filter");
                filter.addActionListener(new
                        ActionListener()
                        {
                            public void actionPerformed(ActionEvent event)
                            {
                                String s = (String)JOptionPane.showInputDialog(
                                                    null, // frame
                                                    "Enter Cutoff Frequency (-6dB point) in Hz\n",
                                                    "Filter",
                                                    JOptionPane.PLAIN_MESSAGE,
                                                    null, // icon
                                                    null,
                                                    "50");

                                //If a string was returned, say so.
                                if ((s != null) && (s.length() > 0)) {
                                    sl.lowpassFilter(new Double(s));
                                    XYPlot plot;
                                    plot = (XYPlot)mainChart.getPlot();
                                    try {
                                        plot.setDataset(sl.getDataset(), 
                                            new XYSeriesCollection(SampleLogXYSeries.makeXYSeries(sl)));
                                    }
                                    catch (Exception e)
                                    {
                                    }
                                }

                            }
                        }
                    );
                menu.add(filter);

                JMenuItem deglitch = new JMenuItem("Deglich Filter");
                deglitch.addActionListener(new
                        ActionListener()
                        {
                            public void actionPerformed(ActionEvent event)
                            {
                                System.out.format("Deglitch\n");
                                sl.deGlitch();
                                XYPlot plot;
                                plot = (XYPlot)mainChart.getPlot();
                                try {
                                    plot.setDataset(sl.getDataset(), 
                                        new XYSeriesCollection(SampleLogXYSeries.makeXYSeries(sl)));
                                } 
                                catch (Exception e)
                                {
                                }
                            }
                        }
                    );
                menu.add(deglitch);
                

                JMenuItem range = new JMenuItem("Adjust Range");
                range.addActionListener(new
                        ActionListener()
                        {
                            public void actionPerformed(ActionEvent event)
                            {
                                JFrame frame = new JFrame("Adjust range for " + sl.GetName());

                                Double min = sl.getParser().getRangeMin();
                                Double max = sl.getParser().getRangeMax();
                                Double curmin = sl.getAxis().getRange().getLowerBound();
                                Double curmax = sl.getAxis().getRange().getUpperBound();
                                if (curmin < min)
                                {
                                    min = curmin;
                                }
                                if (curmax > max)
                                {
                                    max = curmax;
                                }

                                min *= 1000;
                                max *= 1000;
                                curmax *= 1000;
                                curmin *= 1000;
                                JSlider smin = new JSlider(JSlider.HORIZONTAL, 
                                                            min.intValue(),
                                                            max.intValue(),
                                                            curmin.intValue());
                                JSlider smax = new JSlider(JSlider.HORIZONTAL, 
                                                            min.intValue(),
                                                            max.intValue(),
                                                            curmax.intValue());
                                
                                Box minbox = new Box(BoxLayout.Y_AXIS);                                
                                final JTextField tmin = new JTextField(new 
                                        Double(sl.getAxis().getLowerBound()).toString());
                                tmin.addActionListener(new
                                    ActionListener()
                                    {
                                        public void actionPerformed(ActionEvent event)
                                        {
                                            String text = tmin.getText();
                                            Double v = Double.valueOf(text);
                                            sl.getAxis().setLowerBound(v);
                                        }
                                    }
                                );

                                    
                                smin.addChangeListener(new 
                                    ChangeListener()
                                    {
                                        public void stateChanged(ChangeEvent e) {
                                            JSlider source = (JSlider)e.getSource();
                                            if (true || !source.getValueIsAdjusting()) {
                                                int val = (int)source.getValue();
                                                Double dval = new Double(val).doubleValue()/1000;
                                                sl.getAxis().setLowerBound(dval);
                                                tmin.setText(dval.toString());
                                            }
                                        }
                                    }
                                );
                                minbox.add(smin);
                                minbox.add(tmin);
                            
                                Box maxbox = new Box(BoxLayout.Y_AXIS);
                                final JTextField tmax = new JTextField(new 
                                        Double(sl.getAxis().getUpperBound()).toString());
                                tmax.addActionListener(new 
                                    ActionListener()
                                    {
                                        public void actionPerformed(ActionEvent event)
                                        {
                                            String text = tmax.getText();
                                            Double v = Double.valueOf(text);
                                            sl.getAxis().setUpperBound(v);
                                        }
                                    }
                                );
                                
                                smax.addChangeListener(new 
                                    ChangeListener()
                                    {
                                        public void stateChanged(ChangeEvent e) {
                                            JSlider source = (JSlider)e.getSource();
                                            if (true || !source.getValueIsAdjusting()) {
                                                int val = (int)source.getValue();
                                                Double dval = new Double(val).doubleValue()/1000;
                                                sl.getAxis().setUpperBound(dval);
                                                tmax.setText(dval.toString());
                                            }
                                        }
                                    });
                                maxbox.add(smax);
                                maxbox.add(tmax);

                                JPanel panel = new JPanel();
                                panel.setLayout(new BorderLayout());
                                panel.add(minbox, BorderLayout.NORTH);
                                panel.add(maxbox, BorderLayout.SOUTH);
                                frame.getContentPane().add(panel);
                                frame.pack();
                                frame.setVisible(true);
                            }
                        }
                    );
                menu.add(range);

                JMenuItem reset = new JMenuItem("Reset Range");
                reset.addActionListener(new
                        ActionListener()
                        {
                            public void actionPerformed(ActionEvent event)
                            {
                                sl.getAxis().setUpperBound(sl.getParser().getRangeMax());
                                sl.getAxis().setLowerBound(sl.getParser().getRangeMin());
                            }
                        }
                    );
                menu.add(reset);
                
                menu.add(chgcolor);

                if (sl.getParser().hasParameters())
                {
                    JMenuItem parms = new JMenuItem("Trace Parameters");
                    parms.addActionListener(new
                            ActionListener()
                            {
                                public void actionPerformed(ActionEvent event)
                                {
                                    String s = (String)JOptionPane.showInputDialog(
                                                        null, // frame
                                                        "Current: " + sl.getParser().getAdditionalText(),
                                                        "Trace Parameters",
                                                        JOptionPane.PLAIN_MESSAGE,
                                                        null, // icon
                                                        null,
                                                        "50");

                                    sl.getParser().setParameter(1);
                                    updateSingleAxis(sl);
                                }
                            }
                        );
                    menu.add(parms);
                }
            }
            else if (node.isFile())
            {
                JMenuItem remove = new JMenuItem("Remove file");
                menu.add(remove);
            }
            else
            {
                JMenuItem removeall = new JMenuItem("Remove all files");
                menu.add(removeall);
            }
            
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
        else
        {
            System.out.print("Tree click is not popup trigger\n");
        }

    }

    
    private JPanel createParameterSelectorPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        
        MouseListener ml = new MouseAdapter() {
             public void mousePressed(MouseEvent e) {
                    treeClickHandler(e);
             }
             public void mouseReleased(MouseEvent e) {
                    treeClickHandler(e);
             }
        };
        logPanelTree.addMouseListener(ml);

        
        panel.add(logPanelTree);
        
        return panel;            
    }

    private JPanel createDataViewPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        
        JTable table = new JTable(this.tableModel);
        table.setPreferredScrollableViewportSize(new java.awt.Dimension(800, 200));
        table.setOpaque(true);
        panel.add(table);
        return panel;            
    }
    
        
    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createXYLineChart(
            null,
            "Time (ms)", 
            null, null, 
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        return chart;
    }    


    private int nextDataset = 0;
    static Color[]  colors = {Color.black, Color.blue, Color.red, Color.green,
                              Color.gray, Color.magenta, Color.orange, Color.cyan};


    private void loadLogFile(String filename)
    {
        XYPlot plot = (XYPlot) mainChart.getPlot();
        addDatasetsFromLog(filename, plot);
    }
    
    public class LogTreeNode extends DefaultMutableTreeNode {
        public boolean isAxis()
        {
            if (singleAxis != null)
            {
                return true;
            }
            return false;
        }
        public boolean isFile()
        {
            if (multiAxis != null)
            {
                return true;
            }
            return false;
        }

        public String toString()
        {
            return text;
        }
        void setText(String text)
        {
            this.text = text;
        }

        LogTreeNode(MultiAxisSampleLog ml)
        {
            multiAxis = ml;
            singleAxis = null;
        }
        LogTreeNode(SingleAxisSampleLog sl)
        {
            singleAxis = sl;
            multiAxis = null;
        }
        LogTreeNode()
        {
            singleAxis = null;
            multiAxis  = null;
        }

        SingleAxisSampleLog getSingleLog()
        {
            return singleAxis;
        }
        
        SingleAxisSampleLog singleAxis;
        MultiAxisSampleLog  multiAxis;
        String              text;
    }
    
    private void addNodeForFile(MultiAxisSampleLog ml, File file)
    {
         String nodename = file.getName();
         LogTreeNode filenode = new LogTreeNode(ml);
         filenode.setText(nodename);
         
         Iterator<SingleAxisSampleLog> i = ml.iterator();

         LogTreeNode axisnode = null;
         while (i.hasNext())
         {
             SingleAxisSampleLog sl = i.next();
  
             if (sl.isEmpty())
             {
                 continue;
             }
             
             axisnode = new LogTreeNode(sl);
             axisnode.setText(sl.GetName());
             sl.setLogTreeNode(axisnode);
             filenode.add(axisnode);
         }
         
        LogTreeRoot.add(filenode);
        Object path[] = {LogTreeRoot, filenode, axisnode};
        TreePath selPath = new TreePath(path);
        logPanelTree.expandPath(selPath);
        logPanelTree.setSelectionPath(selPath);
        logPanelTree.treeDidChange();
    }

    public MultiAxisSampleLog currentMultiAxis;
    
    /**
     * Creates a sample dataset.
     * 
     * @return A sample dataset.
     */
    private void addDatasetsFromLog(String filename, XYPlot plot) {

        MultiAxisSampleLog ml = new MultiAxisSampleLog();
        File file = new File(filename);
        try {
            ml.LoadFromBinaryFile(file);
        } catch(Exception e) {
            System.out.println("Got exception opening file");
            e.printStackTrace();
            return;
        }

        this.currentMultiAxis = ml;

        addNodeForFile(ml, file);
      
        Iterator<SingleAxisSampleLog> i = ml.iterator();

        while (i.hasNext())
        {
            SingleAxisSampleLog sl = i.next();
 
            if (sl.isEmpty())
            {
                continue;
            }
            
            addSingleAxisSampleLogToChart(sl, nextDataset, plot);
            sl.setDataset(nextDataset);
            ++nextDataset;
        }

        bindScrollbarToRange(ml);
    }
    
//    private Color getColorForLog(String logname)
    
    private class ParameterRangeAxis {
        ParameterRangeAxis(NumberAxis a)
        {
            this.axis = a;
        }
        public  NumberAxis axis;
        int     axis_index_on_chart;
        int     dataset;
    };
    
    private TreeMap<String, ParameterRangeAxis> axisMap = new TreeMap<String, ParameterRangeAxis>();
    
    private boolean isFirstAxis = true;
    
    private void addSingleAxisSampleLogToChart(SingleAxisSampleLog sl, int dataset, XYPlot plot)
    {
        //sl.trimGlitches();
        //sl.lowpass(0.5);

        XYSeries series = null;
        try {
            series = SampleLogXYSeries.makeXYSeries(sl);
        } 
        catch (Exception e)
        {
        }

                
        System.out.print("Finding axis for " + sl.GetName() + "\n");
        
        NumberAxis axis;
        ParameterRangeAxis pra = axisMap.get(sl.GetName());
        if (pra == null)
        {
            System.out.print("Making new axis for " + sl.GetName() + "\n");

            pra = new ParameterRangeAxis(new NumberAxis(sl.GetName()));
            axis = pra.axis;
            
            axis.setLabelPaint(colors[dataset % colors.length]);
            axis.setTickLabelPaint(colors[dataset % colors.length]);
            axisMap.put(sl.GetName(), pra);

            if (isFirstAxis == true)
            {
                // default axis.
                pra.axis_index_on_chart = 0;
                isFirstAxis = false;
            }
            else
            {
                pra.axis_index_on_chart = plot.getRangeAxisCount();
            }

            pra.dataset = dataset;
            
            plot.setRangeAxis(pra.axis_index_on_chart, axis);
            
            if (plot.getRangeAxisCount()%2 == 1)
            {
                plot.setRangeAxisLocation(nextDataset, AxisLocation.TOP_OR_RIGHT);
            }
            else
            {
                plot.setRangeAxisLocation(nextDataset, AxisLocation.BOTTOM_OR_LEFT);
            }
        }
        else
        {
            axis = pra.axis;
        }

        plot.setDataset(dataset, new XYSeriesCollection(series));
        plot.mapDatasetToRangeAxis(dataset, pra.axis_index_on_chart);

        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairLockedOnData(false);
        plot.setRangeCrosshairVisible(true);
        plot.setRangeCrosshairLockedOnData(false);
        
        XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
//            XYStepRenderer rend = new XYStepRenderer();
        rend.setSeriesPaint(0, colors[dataset % colors.length]);
        rend.setSeriesShapesVisible(0, false);

        XYItemLabelGenerator gen = new StandardXYItemLabelGenerator();
        rend.setItemLabelGenerator(gen);
        rend.setItemLabelsVisible(true);
            
        plot.setRenderer(dataset, rend);

        sl.setRenderer(rend);
        sl.setAxis(axis);
        sl.getAxis().setUpperBound(sl.getParser().getRangeStdMax());
        sl.getAxis().setLowerBound(sl.getParser().getRangeStdMin());
    }        


    private void updateSingleAxis(SingleAxisSampleLog sl)
    {
        ParameterRangeAxis pra = axisMap.get(sl.GetName());
        if (pra == null)
        {
            System.out.format("Could not find PRA for %s: can't update\n", sl.GetName());
            return;
        }
        String newname = sl.get(0).GetParameterName();
        currentMultiAxis.RenameAxis(sl.GetName(), newname);
        axisMap.remove(sl.GetName());
        sl.SetName(newname);
        sl.getAxis().setLabel(sl.GetName());
        XYSeries newseries = null;
        try {
            newseries = SampleLogXYSeries.makeXYSeries(sl);
        }
        catch (Exception e)
        {
        }
        axisMap.put(sl.GetName(), pra); // fixme: old one still there
        XYPlot plot = plot = (XYPlot)mainChart.getPlot();
        plot.setDataset(pra.dataset, new XYSeriesCollection(newseries));
        sl.getAxis().setUpperBound(sl.getParser().getRangeStdMax());
        sl.getAxis().setLowerBound(sl.getParser().getRangeStdMin());
    }
    
    static String lastdir = ".";
    static JFileChooser chooser = new JFileChooser();
    {
        chooser.setCurrentDirectory(new File(lastdir));
        //chooser.setCurrentDirectory(new File("c:/p4/TDS/src/TDS-1.3"));
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // first the file menu
        JMenu fileMenu = new JMenu("File", true);
        fileMenu.setMnemonic('F');

        JMenuItem openItem = new JMenuItem("Open", 'o');
        openItem.addActionListener(new
                ActionListener()
                {
                    public void actionPerformed(ActionEvent event)
                    {
                        int result = chooser.showOpenDialog(mainPanel);
                        if (result == JFileChooser.APPROVE_OPTION)
                        {
                            loadLogFile(chooser.getSelectedFile().getPath());
                            lastdir = chooser.getCurrentDirectory().toString();
                        }
                    }
                }
            );            
        fileMenu.add(openItem);
        
        JMenuItem newItem = new JMenuItem("New Window", 'n');
        newItem.addActionListener(new
                ActionListener()
                {
                    public void actionPerformed(ActionEvent event)
                    {
                        newTDSWindow();
                    }
                }
            );
        fileMenu.add(newItem);

        JMenuItem closeItem = new JMenuItem("Close", 'c');
        closeItem.addActionListener(new
                ActionListener()
                {
                    public void actionPerformed(ActionEvent event)
                    {
                        System.out.format("Close; windowCount: %d\n", windowCount);
                        if (windowCount > 1)
                        {
                            dispose();
                        }
                        else
                        {
                            attemptExit();
                        }
                    }
                }
            );
        fileMenu.add(closeItem);
    
        JMenuItem exitItem = new JMenuItem("Exit", 'x');
        exitItem.setActionCommand(EXIT_COMMAND);
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);

        // finally, glue together the menu and return it
        menuBar.add(fileMenu);

        return menuBar;
    }
 

    /**
     * Handles menu selections by passing control to an appropriate method.
     *
     * @param event  the event.
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();
        if (command.equals(EXIT_COMMAND)) {
            attemptExit();
        }
    }
    
    /**
     * Exits the application, but only if the user agrees.
     */
    private void attemptExit() {
        System.exit(0);

        String title = "Confirm";
        String message = "Are you sure you want to exit?";
        int result = JOptionPane.showConfirmDialog(
            this, message, title, JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    private static int windowCount = 0;

    private static TDSLogViewer newTDSWindow() {
        final TDSLogViewer tds = new TDSLogViewer();
        tds.pack();
        RefineryUtilities.centerFrameOnScreen(tds);
        tds.setVisible(true);
        ++windowCount;
        System.out.format("New; windowCount: %d\n", windowCount);

        tds.addWindowListener(new 
                WindowListener()
                {
                    public void windowClosing(WindowEvent e)
                    {
                        System.out.format("Window closing\n");
                        tds.dispose();
                    }
                    public void windowClosed(WindowEvent e)
                    {
                        --windowCount;
                        System.out.format("close notify; windowCount: %d\n", windowCount);
                        if (windowCount == 0)
                        {
                            System.exit(0);
                        }
                    }
                    public void windowOpened(WindowEvent e)
                    {
                    }
                    public void windowIconified(WindowEvent e)
                    {
                    }
                    public void windowDeiconified(WindowEvent e)
                    {
                    }
                    public void windowActivated(WindowEvent e)
                    {
                    }
                    public void windowDeactivated(WindowEvent e)
                    {
                    }
                    public void windowGainedFocus(WindowEvent e)
                    {
                    }
                    public void windowLostFocus(WindowEvent e)
                    {
                    }
                    public void windowStateChanged(WindowEvent e)
                    {
                    }
                }
            );


        return tds;
    }        

    public static void main(String[] args) {
        TDSLogViewer tds = newTDSWindow();

        if (args.length > 0)
        {
            tds.loadLogFile(args[0]);
        }
    }
 

    static class TDSChartMouseListener implements ChartMouseListener {
        
        ChartPanel panel;
        TDSLogViewer tds;
        
        /**
         * Creates a new mouse listener.
         * 
         * @param panel  the panel.
         */
        public TDSChartMouseListener(ChartPanel panel, TDSLogViewer tds) {
            this.panel = panel;    
            this.tds = tds;
        }
        /**
         * Callback method for receiving notification of a mouse click on a 
         * chart.
         *
         * @param event  information about the event.
         */
        public void chartMouseClicked(ChartMouseEvent event) {
            int x = event.getTrigger().getX(); 
            int y = event.getTrigger().getY();
            
            // the following translation takes account of the fact that the 
            // chart image may have been scaled up or down to fit the panel...
            Point2D p = this.panel.translateScreenToJava2D(new Point(x, y));

            tds.tableModel.clearSampleInfo();
            Iterator<String> i = tds.axisMap.keySet().iterator();
            while(i.hasNext())
            {
                String key = i.next();
                System.out.print("Map has entry for key " + key + "\n");

                ParameterRangeAxis pra = tds.axisMap.get(key);
                NumberAxis range = pra.axis;

                // now convert the Java2D coordinate to axis coordinates...
                XYPlot plot = (XYPlot) this.panel.getChart().getPlot();
                ChartRenderingInfo info = this.panel.getChartRenderingInfo();
                Rectangle2D dataArea = info.getPlotInfo().getDataArea();
                double xx = plot.getDomainAxis().java2DToValue(p.getX(), dataArea, 
                        plot.getDomainAxisEdge());
                double yy = range.java2DToValue(p.getY(), dataArea, 
                        plot.getRangeAxisEdge());

                System.out.println("Mouse coordinates are (" + x + ", " + y 
                    + "), in data space for range " + key + ": " + xx + ", " 
                    + yy + ")\n");

                Double t = new Double(xx);                
                Double actual = tds.currentMultiAxis.mapTimeToAxisValue(t, key);


                tds.tableModel.setSampleInfo(t, key, actual, yy);


            }
        }

        /**
         * Callback method for receiving notification of a mouse movement on a 
         * chart.
         *
         * @param event  information about the event.
         */
        public void chartMouseMoved(ChartMouseEvent event) {  
            // ignore
        }
        
    }


    static class LogTableModel extends AbstractTableModel 
            implements TableModel {


        private TreeMap<String, String> actualMap = new TreeMap<String, String>();
        private TreeMap<String, String> mouseMap = new TreeMap<String, String>();
        private String time;

        public LogTableModel()
        {
        }

        public int getColumnCount() {
            Iterator<String> i = actualMap.keySet().iterator();
            int cols = 1;
            while(i.hasNext())
            {
                ++cols;
                i.next();
            }            

            return cols;
        }
        
        public int getRowCount() {
            return 3;
        }
        
        public String getColumnName(int col) {
            return (String)getValueAt(0, col);
        }

        private static String dStr(Double v)
        {
            v *= 100.0;
            Long vl = Math.round(v);
            v = vl/100.0;
            return v.toString();
        }

        public void clearSampleInfo()
        {
            this.actualMap.clear();
            this.mouseMap.clear();
        }
        public void setSampleInfo(Double time, String key, Double actualValue, 
                                  Double mouseValue)
        {
            this.time = dStr(time);
            this.actualMap.put(key, dStr(actualValue));
            this.mouseMap.put(key, dStr(mouseValue));
            fireTableStructureChanged();
            fireTableDataChanged();
        }
        public Object getValueAt(int row, int col) {
            if (col == 0)
            {
                if (row == 0)
                {
                    return "";
                }
                else if (row == 1)
                {
                    return "At time " + this.time;
                }
                else
                {
                    return "At click location";
                }
            }
            Iterator<String> i = actualMap.keySet().iterator();
            int c = 1;
            while(i.hasNext())
            {
                String name = i.next();
                if (c == col)
                {
                    if (row == 0)
                    {
                        return name;
                    }
                    else if (row == 2)
                    {
                        return mouseMap.get(name);
                    }
                    else                        
                    {
                        return actualMap.get(name);
                    }
                }
                ++c;
            }
            return "?";
        }
    }
    
    public LogTableModel tableModel = new LogTableModel();
 


}



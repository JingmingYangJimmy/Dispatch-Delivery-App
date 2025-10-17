import React, { useState, useEffect } from 'react';
import { MapPin, Package, Truck, Plane, DollarSign, Clock, Battery } from 'lucide-react';

// API配置
const API_BASE_URL = 'http://localhost:8080/api';

const DeliveryPathRecommendationApp = () => {
  const [orders, setOrders] = useState([]);
  const [stations, setStations] = useState([]);
  const [devices, setDevices] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const [activeTab, setActiveTab] = useState('create');

  // 新订单表单状态
  const [newOrder, setNewOrder] = useState({
    originLat: 40.7580,
    originLng: -73.9855,
    originAddress: 'Manhattan Central, NY',
    destLat: 40.6782,
    destLng: -73.9442,
    destAddress: 'Brooklyn, NY',
    weight: 2.0
  });

  const [loading, setLoading] = useState(false);

  // 获取站点信息
  const fetchStations = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/stations`);
      const data = await response.json();
      setStations(data.stations || []);
    } catch (error) {
      console.error('获取站点失败:', error);
    }
  };

  // 获取可用设备
  const fetchAvailableDevices = async (lat, lng, radius = 5) => {
    try {
      const response = await fetch(
          `${API_BASE_URL}/devices/available?lat=${lat}&lng=${lng}&radius=${radius}`
      );
      const data = await response.json();
      setDevices(data.devices || []);
    } catch (error) {
      console.error('获取设备失败:', error);
    }
  };

  // 提交路径推荐请求
  const requestPathRecommendation = async () => {
    setLoading(true);
    try {
      const orderId = `ORD${Date.now()}`;
      const response = await fetch(`${API_BASE_URL}/path/recommend`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          orderId,
          origin: {
            lat: newOrder.originLat,
            lng: newOrder.originLng,
            address: newOrder.originAddress
          },
          destination: {
            lat: newOrder.destLat,
            lng: newOrder.destLng,
            address: newOrder.destAddress
          },
          packageWeight: newOrder.weight
        })
      });

      const data = await response.json();
      setRecommendations(data.recommendations || []);
      setSelectedOrder({ orderId, ...newOrder });
      setActiveTab('results');
    } catch (error) {
      console.error('获取路径推荐失败:', error);
      alert('获取路径推荐失败，请检查后端服务');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStations();
    fetchAvailableDevices(40.7580, -73.9855);
  }, []);

  // 渲染设备图标
  const DeviceIcon = ({ type }) => {
    return type === 'DRONE' ?
        <Plane className="w-5 h-5 text-blue-500" /> :
        <Truck className="w-5 h-5 text-green-500" />;
  };

  // 渲染推荐类型标签
  const RecommendationBadge = ({ type }) => {
    const colors = {
      FASTEST: 'bg-red-100 text-red-800',
      CHEAPEST: 'bg-green-100 text-green-800',
      OPTIMAL: 'bg-blue-100 text-blue-800'
    };
    return (
        <span className={`px-3 py-1 rounded-full text-sm font-semibold ${colors[type]}`}>
        {type === 'FASTEST' ? '最快' : type === 'CHEAPEST' ? '最便宜' : '最优'}
      </span>
    );
  };

  return (
      <div className="min-h-screen bg-gray-50">
        {/* 顶部导航 */}
        <nav className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16 items-center">
              <div className="flex items-center space-x-3">
                <Package className="w-8 h-8 text-blue-600" />
                <h1 className="text-2xl font-bold text-gray-900">NYC配送路径推荐系统</h1>
              </div>
              <div className="flex space-x-4">
                <button
                    onClick={() => setActiveTab('create')}
                    className={`px-4 py-2 rounded-lg font-medium transition ${
                        activeTab === 'create'
                            ? 'bg-blue-600 text-white'
                            : 'text-gray-600 hover:bg-gray-100'
                    }`}
                >
                  创建订单
                </button>
                <button
                    onClick={() => setActiveTab('stations')}
                    className={`px-4 py-2 rounded-lg font-medium transition ${
                        activeTab === 'stations'
                            ? 'bg-blue-600 text-white'
                            : 'text-gray-600 hover:bg-gray-100'
                    }`}
                >
                  站点信息
                </button>
                <button
                    onClick={() => setActiveTab('devices')}
                    className={`px-4 py-2 rounded-lg font-medium transition ${
                        activeTab === 'devices'
                            ? 'bg-blue-600 text-white'
                            : 'text-gray-600 hover:bg-gray-100'
                    }`}
                >
                  可用设备
                </button>
              </div>
            </div>
          </div>
        </nav>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* 创建订单页面 */}
          {activeTab === 'create' && (
              <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-xl font-bold text-gray-900 mb-6">创建配送订单</h2>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* 起点信息 */}
                  <div className="space-y-4">
                    <h3 className="font-semibold text-gray-700 flex items-center">
                      <MapPin className="w-5 h-5 mr-2 text-green-600" />
                      起点信息
                    </h3>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">地址</label>
                      <input
                          type="text"
                          value={newOrder.originAddress}
                          onChange={(e) => setNewOrder({...newOrder, originAddress: e.target.value})}
                          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">纬度</label>
                        <input
                            type="number"
                            step="0.0001"
                            value={newOrder.originLat}
                            onChange={(e) => setNewOrder({...newOrder, originLat: parseFloat(e.target.value)})}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">经度</label>
                        <input
                            type="number"
                            step="0.0001"
                            value={newOrder.originLng}
                            onChange={(e) => setNewOrder({...newOrder, originLng: parseFloat(e.target.value)})}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* 终点信息 */}
                  <div className="space-y-4">
                    <h3 className="font-semibold text-gray-700 flex items-center">
                      <MapPin className="w-5 h-5 mr-2 text-red-600" />
                      终点信息
                    </h3>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">地址</label>
                      <input
                          type="text"
                          value={newOrder.destAddress}
                          onChange={(e) => setNewOrder({...newOrder, destAddress: e.target.value})}
                          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">纬度</label>
                        <input
                            type="number"
                            step="0.0001"
                            value={newOrder.destLat}
                            onChange={(e) => setNewOrder({...newOrder, destLat: parseFloat(e.target.value)})}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">经度</label>
                        <input
                            type="number"
                            step="0.0001"
                            value={newOrder.destLng}
                            onChange={(e) => setNewOrder({...newOrder, destLng: parseFloat(e.target.value)})}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* 包裹重量 */}
                <div className="mt-6">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    包裹重量 (kg)
                  </label>
                  <input
                      type="number"
                      step="0.1"
                      value={newOrder.weight}
                      onChange={(e) => setNewOrder({...newOrder, weight: parseFloat(e.target.value)})}
                      className="w-full max-w-xs px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                  <p className="mt-2 text-sm text-gray-500">
                    轻量级: 0-1kg | 中量级: 1-3kg | 重量级: 3-5kg | 超重: &gt;5kg
                  </p>
                </div>

                {/* 提交按钮 */}
                <div className="mt-8">
                  <button
                      onClick={requestPathRecommendation}
                      disabled={loading}
                      className="w-full md:w-auto px-8 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed"
                  >
                    {loading ? '计算中...' : '获取路径推荐'}
                  </button>
                </div>
              </div>
          )}

          {/* 推荐结果页面 */}
          {activeTab === 'results' && (
              <div className="space-y-6">
                <div className="bg-white rounded-lg shadow-md p-6">
                  <h2 className="text-xl font-bold text-gray-900 mb-4">订单信息</h2>
                  {selectedOrder && (
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                        <div>
                          <span className="text-gray-600">订单号:</span>
                          <span className="ml-2 font-semibold">{selectedOrder.orderId}</span>
                        </div>
                        <div>
                          <span className="text-gray-600">起点:</span>
                          <span className="ml-2 font-semibold">{selectedOrder.originAddress}</span>
                        </div>
                        <div>
                          <span className="text-gray-600">终点:</span>
                          <span className="ml-2 font-semibold">{selectedOrder.destAddress}</span>
                        </div>
                        <div>
                          <span className="text-gray-600">包裹重量:</span>
                          <span className="ml-2 font-semibold">{selectedOrder.weight} kg</span>
                        </div>
                      </div>
                  )}
                </div>

                {/* 推荐方案 */}
                {recommendations.map((rec, index) => (
                    <div key={index} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-4">
                          <DeviceIcon type={rec.device} />
                          <div>
                            <h3 className="text-lg font-bold text-gray-900">
                              {rec.device === 'DRONE' ? '无人机配送' : '机器人配送'}
                            </h3>
                            <p className="text-sm text-gray-600">设备ID: {rec.deviceId}</p>
                          </div>
                        </div>
                        <RecommendationBadge type={rec.type} />
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                        <div className="flex items-center space-x-2">
                          <Clock className="w-5 h-5 text-gray-400" />
                          <div>
                            <p className="text-xs text-gray-500">预计时间</p>
                            <p className="text-lg font-bold text-gray-900">{rec.route.duration} 分钟</p>
                          </div>
                        </div>
                        <div className="flex items-center space-x-2">
                          <MapPin className="w-5 h-5 text-gray-400" />
                          <div>
                            <p className="text-xs text-gray-500">距离</p>
                            <p className="text-lg font-bold text-gray-900">{rec.route.distance.toFixed(2)} km</p>
                          </div>
                        </div>
                        <div className="flex items-center space-x-2">
                          <DollarSign className="w-5 h-5 text-gray-400" />
                          <div>
                            <p className="text-xs text-gray-500">费用</p>
                            <p className="text-lg font-bold text-gray-900">${rec.route.cost.toFixed(2)}</p>
                          </div>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Battery className="w-5 h-5 text-gray-400" />
                          <div>
                            <p className="text-xs text-gray-500">综合得分</p>
                            <p className="text-lg font-bold text-gray-900">{(rec.score.totalScore * 100).toFixed(0)}%</p>
                          </div>
                        </div>
                      </div>

                      <div className="bg-gray-50 rounded-lg p-4">
                        <p className="text-sm text-gray-600 mb-2">评分详情:</p>
                        <div className="space-y-2">
                          <div className="flex items-center">
                            <span className="text-xs text-gray-500 w-24">时间得分:</span>
                            <div className="flex-1 bg-gray-200 rounded-full h-2 mr-2">
                              <div
                                  className="bg-blue-500 h-2 rounded-full"
                                  style={{width: `${rec.score.timeScore * 100}%`}}
                              />
                            </div>
                            <span className="text-xs font-semibold">{(rec.score.timeScore * 100).toFixed(0)}%</span>
                          </div>
                          <div className="flex items-center">
                            <span className="text-xs text-gray-500 w-24">成本得分:</span>
                            <div className="flex-1 bg-gray-200 rounded-full h-2 mr-2">
                              <div
                                  className="bg-green-500 h-2 rounded-full"
                                  style={{width: `${rec.score.costScore * 100}%`}}
                              />
                            </div>
                            <span className="text-xs font-semibold">{(rec.score.costScore * 100).toFixed(0)}%</span>
                          </div>
                        </div>
                      </div>

                      <button className="mt-4 w-full py-2 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition">
                        选择此方案
                      </button>
                    </div>
                ))}

                {recommendations.length === 0 && (
                    <div className="bg-white rounded-lg shadow-md p-12 text-center">
                      <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                      <p className="text-gray-500">暂无推荐方案</p>
                    </div>
                )}
              </div>
          )}

          {/* 站点信息页面 */}
          {activeTab === 'stations' && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {stations.map((station) => (
                    <div key={station.stationId} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
                      <h3 className="text-lg font-bold text-gray-900 mb-4">{station.name}</h3>
                      <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                          <span className="text-gray-600">站点ID:</span>
                          <span className="font-semibold">{station.stationId}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-gray-600">位置:</span>
                          <span className="font-semibold">
                      {station.location.lat.toFixed(4)}, {station.location.lng.toFixed(4)}
                    </span>
                        </div>
                        <div className="flex justify-between items-center">
                    <span className="text-gray-600 flex items-center">
                      <Plane className="w-4 h-4 mr-1" />
                      可用无人机:
                    </span>
                          <span className="font-semibold text-blue-600">{station.availableDrones}</span>
                        </div>
                        <div className="flex justify-between items-center">
                    <span className="text-gray-600 flex items-center">
                      <Truck className="w-4 h-4 mr-1" />
                      可用机器人:
                    </span>
                          <span className="font-semibold text-green-600">{station.availableRobots}</span>
                        </div>
                      </div>
                    </div>
                ))}
              </div>
          )}

          {/* 设备信息页面 */}
          {activeTab === 'devices' && (
              <div className="bg-white rounded-lg shadow-md overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        设备ID
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        类型
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        所属站点
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        电量
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        最大载重
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        状态
                      </th>
                    </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                    {devices.map((device) => (
                        <tr key={device.deviceId} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                            {device.deviceId}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm">
                            <div className="flex items-center">
                              <DeviceIcon type={device.type} />
                              <span className="ml-2">{device.type}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {device.stationId}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm">
                            <div className="flex items-center">
                              <Battery className="w-4 h-4 mr-2 text-gray-400" />
                              <span className={device.battery < 30 ? 'text-red-600 font-semibold' : 'text-gray-900'}>
                            {device.battery}%
                          </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                            {device.maxWeight} kg
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                            device.status === 'AVAILABLE'
                                ? 'bg-green-100 text-green-800'
                                : device.status === 'BUSY'
                                    ? 'bg-yellow-100 text-yellow-800'
                                    : 'bg-gray-100 text-gray-800'
                        }`}>
                          {device.status}
                        </span>
                          </td>
                        </tr>
                    ))}
                    </tbody>
                  </table>
                </div>
              </div>
          )}
        </div>
      </div>
  );
};

export default DeliveryPathRecommendationApp;
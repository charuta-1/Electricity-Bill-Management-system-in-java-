import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  LineElement,
  LinearScale,
  CategoryScale,
  PointElement,
  Tooltip,
  Legend
} from 'chart.js';
import api from '../../api/axiosConfig.js';

ChartJS.register(LineElement, LinearScale, CategoryScale, PointElement, Tooltip, Legend);

const UsageChart = ({ compact }) => {
  const [dataPoints, setDataPoints] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchConsumption = async () => {
      try {
        const response = await api.get('/customers/self/consumption');
        setDataPoints(response.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load consumption history');
      }
    };

    fetchConsumption();
  }, []);

  const chartData = {
    labels: dataPoints.map((point) => point.month),
    datasets: [
      {
        label: 'Units Consumed (kWh)',
        data: dataPoints.map((point) => point.units),
        borderColor: '#2563eb',
        backgroundColor: 'rgba(37, 99, 235, 0.15)',
        tension: 0.35,
        fill: true,
        pointRadius: 4
      }
    ]
  };

  const chartOptions = {
    responsive: true,
    plugins: {
      legend: { position: 'bottom' }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value) => `${value} kWh`
        }
      }
    }
  };

  if (compact) {
    return (
      <div>
        {error && <div className="alert alert-danger">{error}</div>}
        {dataPoints.length === 0 ? (
          <p className="text-muted text-center mb-0">No consumption data available yet.</p>
        ) : (
          <Line data={chartData} options={chartOptions} height={220} />
        )}
      </div>
    );
  }

  return (
    <div className="container py-4">
      <h1 className="h4 mb-3">Usage Analytics</h1>
      <p className="text-muted">Monitor monthly consumption trends and optimize your usage.</p>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card card-shadow">
        <div className="card-body">
          {dataPoints.length === 0 ? (
            <p className="text-muted text-center mb-0">No consumption data available yet.</p>
          ) : (
            <Line data={chartData} options={chartOptions} height={300} />
          )}
        </div>
      </div>
    </div>
  );
};

UsageChart.propTypes = {
  compact: PropTypes.bool
};

UsageChart.defaultProps = {
  compact: false
};

export default UsageChart;
